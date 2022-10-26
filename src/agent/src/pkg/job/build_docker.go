package job

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/api"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/logs"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/upgrade/download"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/api/types/mount"
	"github.com/docker/docker/client"
	"github.com/pkg/errors"
	"io"
	"os"
	"strings"
	"sync"
	"time"
)

// buildDockerManager docker构建机构建对象管理
type buildDockerManager struct {
	// instances 正在执行中的构建对象 [string]*api.ThirdPartyDockerTaskInfo
	instances sync.Map
}

func (b *buildDockerManager) GetInstanceCount() int {
	var i = 0
	b.instances.Range(func(key, value interface{}) bool {
		i++
		return true
	})
	return i
}

func (b *buildDockerManager) GetInstances() []api.ThirdPartyDockerTaskInfo {
	result := make([]api.ThirdPartyDockerTaskInfo, 0)
	b.instances.Range(func(key, value interface{}) bool {
		result = append(result, *value.(*api.ThirdPartyDockerTaskInfo))
		return true
	})
	return result
}

func (b *buildDockerManager) AddBuild(buildId string, info *api.ThirdPartyDockerTaskInfo) {
	b.instances.Store(buildId, info)
}

func (b *buildDockerManager) RemoveBuild(buildId string) {
	b.instances.Delete(buildId)
}

var GBuildDockerManager *buildDockerManager

func init() {
	GBuildDockerManager = new(buildDockerManager)
}

const (
	entryPointCmd               = "/data/init.sh"
	localDockerBuildTmpDirName  = "docker_build_tmp"
	LocalDockerWorkSpaceDirName = "docker_workspace"
	dockerDataDir               = "/data/landun/workspace"
	dockerLogDir                = "/data/logs"
)

func runDockerBuild(buildInfo *api.ThirdPartyBuildInfo) {
	if !systemutil.IsLinux() {
		GBuildDockerManager.RemoveBuild(buildInfo.BuildId)
		dockerBuildFinish(&api.ThirdPartyBuildWithStatus{
			ThirdPartyBuildInfo: *buildInfo, Message: "目前仅支持linux系统使用docker构建机",
		})
		return
	}
	// 第一次使用docker构建机后，则直接开启docker构建机相关逻辑
	if !config.GAgentConfig.EnableDockerBuild {
		config.GAgentConfig.EnableDockerBuild = true
		go config.GAgentConfig.SaveConfig()
	}

	// 兼容旧数据，对于没有docker文件的需要重新下载，防止重复下载所以放到主流程做
	if _, err := os.Stat(config.GetDockerInitFilePath()); err != nil {
		if os.IsNotExist(err) {
			_, err = download.DownloadDockerInitFile(systemutil.GetWorkDir())
			if err != nil {
				GBuildDockerManager.RemoveBuild(buildInfo.BuildId)
				dockerBuildFinish(&api.ThirdPartyBuildWithStatus{
					ThirdPartyBuildInfo: *buildInfo, Message: "下载Docker构建机初始化脚本失败|" + err.Error(),
				})
				return
			}
		} else {
			GBuildDockerManager.RemoveBuild(buildInfo.BuildId)
			dockerBuildFinish(&api.ThirdPartyBuildWithStatus{
				ThirdPartyBuildInfo: *buildInfo, Message: "获取Docker构建机初始化脚本状态失败|" + err.Error(),
			})
			return
		}
	}

	go doDockerJob(buildInfo)
}

// doDockerJob 使用docker启动构建
func doDockerJob(buildInfo *api.ThirdPartyBuildInfo) {
	// 各种情况退出时减运行任务数量
	defer func() {
		GBuildDockerManager.RemoveBuild(buildInfo.BuildId)
	}()

	dockerBuildInfo := buildInfo.DockerBuildInfo
	ctx := context.Background()
	cli, err := client.NewClientWithOpts(client.FromEnv, client.WithAPIVersionNegotiation())
	if err != nil {
		logs.Error("DOCKER_JOB|create docker client error ", err)
		dockerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: "创建docker客户端链接错误|" + err.Error()})
		return
	}

	imageName := dockerBuildInfo.Image

	// 判断本地是否已经有镜像了
	images, err := cli.ImageList(ctx, types.ImageListOptions{})
	if err != nil {
		logs.Error("DOCKER_JOB|list docker images error ", err)
		dockerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: "获取docker镜像列表错误|" + err.Error()})
		return
	}
	localExist := false
	imageStr := strings.TrimPrefix(strings.TrimPrefix(imageName, "http://"), "https://")
	for _, image := range images {
		for _, tagName := range image.RepoTags {
			if tagName == imageStr {
				localExist = true
			}
		}
	}

	// 本地没有镜像的需要拉取新的镜像
	if !localExist {
		postLog(false, "开始拉取镜像，镜像名称："+imageName, buildInfo)
		postLog(false, "过大的镜像拉取时间过长（超过3分钟）可能会导致构建机重新领取任务，等待即可，镜像拉取完即可正常执行。也可以预先在本地拉取，构建机会直接启动已存在镜像。"+imageName, buildInfo)
		reader, err := cli.ImagePull(ctx, imageName, types.ImagePullOptions{
			RegistryAuth: generateDockerAuth(dockerBuildInfo.Credential),
		})
		if err != nil {
			logs.Error(fmt.Sprintf("DOCKER_JOB|pull new image %s error ", imageName), err)
			dockerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: fmt.Sprintf("拉取镜像 %s 失败|", imageName) + err.Error()})
			return
		}
		defer reader.Close()
		buf := new(strings.Builder)
		_, err = io.Copy(buf, reader)
		if err != nil {
			logs.Error("DOCKER_JOB|write image message error ", err)
			postLog(true, "获取拉取镜像信息日志失败："+err.Error(), buildInfo)
		} else {
			postLog(false, buf.String(), buildInfo)
		}
	} else {
		postLog(false, "本地存在镜像，准备启动构建环境..."+imageName, buildInfo)
	}

	// 创建docker构建机运行准备空间，拉取docker构建机初始化文件
	tmpDir := fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), localDockerBuildTmpDirName)
	err = mkDir(tmpDir)
	if err != nil {
		errMsg := fmt.Sprintf("创建Docker构建临时目录失败(create tmp directory failed): %s", err.Error())
		logs.Error("DOCKER_JOB|" + errMsg)
		dockerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: errMsg})
		return
	}

	// 创建容器
	containerName := fmt.Sprintf("dispatch-%s-%s-%s", buildInfo.BuildId, buildInfo.VmSeqId, util.RandStringRunes(8))
	mounts, err := parseContainerMounts(buildInfo)
	if err != nil {
		logs.Error("DOCKER_JOB| ", err)
		dockerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: err.Error()})
		return
	}
	var resources container.Resources
	if dockerBuildInfo.DockerResource != nil {
		resources = container.Resources{
			Memory:    dockerBuildInfo.DockerResource.MemoryLimitBytes,
			CPUQuota:  dockerBuildInfo.DockerResource.CpuQuota,
			CPUPeriod: dockerBuildInfo.DockerResource.CpuPeriod,
		}
	}
	hostConfig := &container.HostConfig{
		CapAdd:      []string{"SYS_PTRACE"},
		Mounts:      mounts,
		NetworkMode: container.NetworkMode("bridge"),
		Resources:   resources,
	}

	creatResp, err := cli.ContainerCreate(ctx, &container.Config{
		Image: imageStr,
		Cmd:   []string{"/bin/sh", entryPointCmd},
		Env:   parseContainerEnv(dockerBuildInfo),
	}, hostConfig, nil, nil, containerName)
	if err != nil {
		logs.Error(fmt.Sprintf("DOCKER_JOB|create container %s error ", containerName), err)
		dockerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: fmt.Sprintf("创建容器 %s 失败|", containerName) + err.Error()})
		return
	}

	// 启动容器
	if err := cli.ContainerStart(ctx, creatResp.ID, types.ContainerStartOptions{}); err != nil {
		logs.Error(fmt.Sprintf("DOCKER_JOB|start container %s error ", creatResp.ID), err)
		dockerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: fmt.Sprintf("启动容器 %s 失败|", containerName) + err.Error()})
		return
	}

	// 等待容器结束
	statusCh, errCh := cli.ContainerWait(ctx, creatResp.ID, container.WaitConditionNotRunning)
	select {
	case err := <-errCh:
		if err != nil {
			logs.Error(fmt.Sprintf("DOCKER_JOB|wait container %s over error ", creatResp.ID), err)
			dockerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Message: fmt.Sprintf("等待容器 %s 结束错误|", containerName) + err.Error()})
			if err = cli.ContainerRemove(ctx, creatResp.ID, types.ContainerRemoveOptions{Force: true}); err != nil {
				logs.Error(fmt.Sprintf("DOCKER_JOB|remove container %s error ", creatResp.ID), err)
			}
			return
		}
	case <-statusCh:
	}

	dockerBuildFinish(&api.ThirdPartyBuildWithStatus{ThirdPartyBuildInfo: *buildInfo, Success: true, Message: ""})
	if err = cli.ContainerRemove(ctx, creatResp.ID, types.ContainerRemoveOptions{Force: true}); err != nil {
		logs.Error(fmt.Sprintf("DOCKER_JOB|remove container %s error ", creatResp.ID), err)
	}
}

// dockerBuildFinish docker构建结束相关
func dockerBuildFinish(buildInfo *api.ThirdPartyBuildWithStatus) {
	if buildInfo == nil {
		logs.Warn("DOCKER_JOB|buildInfo not exist")
		return
	}

	if buildInfo.Success {
		time.Sleep(8 * time.Second)
	}
	result, err := api.WorkerBuildFinish(buildInfo)
	if err != nil {
		logs.Error("DOCKER_JOB|send worker build finish failed: ", err.Error())
	}
	if result.IsNotOk() {
		logs.Error("DOCKER_JOB|send worker build finish failed: ", result.Message)
	}
	logs.Info("DOCKER_JOB|workerBuildFinish done")
}

// postLog 向后台上报日志
func postLog(red bool, message string, buildInfo *api.ThirdPartyBuildInfo) {
	taskId := "startVM-" + buildInfo.VmSeqId

	logMessage := &api.LogMessage{
		Message:      message,
		Timestamp:    time.Now().UnixMilli(),
		Tag:          taskId,
		JobId:        buildInfo.ContainerHashId,
		LogType:      api.LogtypeLog,
		ExecuteCount: buildInfo.ExecuteCount,
		SubTag:       nil,
	}

	var err error
	if red {
		_, err = api.AddLogRedLine(buildInfo.BuildId, logMessage, buildInfo.VmSeqId)
	} else {
		_, err = api.AddLogLine(buildInfo.BuildId, logMessage, buildInfo.VmSeqId)
	}
	if err != nil {
		logs.Error("DOCKER_JOB|api post log error", err)
	}
}

// mkDir 与  systemutil.MkBuildTmpDir 功能一致
func mkDir(dir string) error {
	err := os.MkdirAll(dir, os.ModePerm)
	err2 := systemutil.Chmod(dir, os.ModePerm)
	if err == nil && err2 != nil {
		err = err2
	}
	return err
}

// generateDockerAuth 创建拉取docker凭据
func generateDockerAuth(cred *api.Credential) string {
	if cred == nil || cred.User == "" || cred.Password == "" {
		return ""
	}

	authConfig := types.AuthConfig{
		Username: cred.User,
		Password: cred.Password,
	}
	encodedJSON, err := json.Marshal(authConfig)
	if err != nil {
		panic(err)
	}

	return base64.URLEncoding.EncodeToString(encodedJSON)
}

// parseContainerMounts 解析生成容器挂载内容
func parseContainerMounts(buildInfo *api.ThirdPartyBuildInfo) ([]mount.Mount, error) {
	var mounts []mount.Mount

	// 默认绑定本机的java用来执行worker，因为仅支持linux容器所以仅限linux构建机绑定
	if systemutil.IsLinux() {
		javaDir := config.GetJavaDir()
		mounts = append(mounts, mount.Mount{
			Type:     mount.TypeBind,
			Source:   javaDir,
			Target:   "/usr/local/jre",
			ReadOnly: true,
		})
	}

	// 挂载docker构建机初始化脚本
	workDir := systemutil.GetWorkDir()
	mounts = append(mounts, mount.Mount{
		Type:     mount.TypeBind,
		Source:   config.GetDockerInitFilePath(),
		Target:   entryPointCmd,
		ReadOnly: true,
	})

	// 创建并挂载data和log
	dataDir := fmt.Sprintf("%s/%s/data/%s/%s", workDir, LocalDockerWorkSpaceDirName, buildInfo.PipelineId, buildInfo.VmSeqId)
	err := mkDir(dataDir)
	if err != nil && !os.IsExist(err) {
		return nil, errors.Wrapf(err, "create local data dir %s error", dataDir)
	}
	mounts = append(mounts, mount.Mount{
		Type:     mount.TypeBind,
		Source:   dataDir,
		Target:   dockerDataDir,
		ReadOnly: false,
	})

	logsDir := fmt.Sprintf("%s/%s/logs/%s/%s", workDir, LocalDockerWorkSpaceDirName, buildInfo.BuildId, buildInfo.VmSeqId)
	err = mkDir(logsDir)
	if err != nil && !os.IsExist(err) {
		return nil, errors.Wrapf(err, "create local logs dir %s error", logsDir)
	}
	mounts = append(mounts, mount.Mount{
		Type:     mount.TypeBind,
		Source:   logsDir,
		Target:   dockerLogDir,
		ReadOnly: false,
	})

	return mounts, nil
}

// parseContainerEnv 解析生成容器环境变量
func parseContainerEnv(dockerBuildInfo *api.ThirdPartyDockerBuildInfo) []string {
	var envs []string

	// 默认传入环境变量用来构建
	envs = append(envs, "devops_project_id="+config.GAgentConfig.ProjectId)
	envs = append(envs, "devops_agent_id="+dockerBuildInfo.AgentId)
	envs = append(envs, "devops_agent_secret_key="+dockerBuildInfo.SecretKey)
	envs = append(envs, "devops_gateway="+config.GetGateWay())
	// 通过环境变量区分agent docker
	envs = append(envs, "agent_build_env=DOCKER")

	if dockerBuildInfo.Envs == nil {
		return envs
	}

	for k, v := range dockerBuildInfo.Envs {
		envs = append(envs, k+"="+v)
	}

	return envs
}
