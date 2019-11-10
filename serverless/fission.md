# Fission Deep Dive

本文主要是围绕Fission如何在k8s之上实现其“承诺的”业务水平，主要从按照其模块进行进一步的探索，与相对的操作的现象进行对比。

本文主要的内容以[该篇文章](https://www.alibabacloud.com/blog/fission-a-deep-dive-into-serverless-kubernetes-frameworks-2_594902)为纲，结合一些自己的fission实践进行相应的补充和源码的理解。

## 关于Fission的权限

Fission初始化之后会创建如下权限相关的角色

fission-admin(RoleBinding):

```yaml
Name:         fission-admin
Labels:       <none>
Annotations:  kubectl.kubernetes.io/last-applied-configuration:
                {"apiVersion":"rbac.authorization.k8s.io/v1","kind":"RoleBinding","metadata":{"annotations":{},"name":"fission-admin","namespace":"default...
Role:
  Kind:  ClusterRole
  Name:  admin
Subjects:
  Kind            Name         Namespace
  ----            ----         ---------
  ServiceAccount  fission-svc  default
```

fission-svc(ServiceAccount):

```yaml
Name:                fission-svc
Namespace:           default
Labels:              <none>
Annotations:         kubectl.kubernetes.io/last-applied-configuration:
                       {"apiVersion":"v1","kind":"ServiceAccount","metadata":{"annotations":{},"name":"fission-svc","namespace":"default"}}
Image pull secrets:  <none>
Mountable secrets:   fission-svc-token-vbbt9
Tokens:              fission-svc-token-vbbt9
Events:              <none>
```

admin(ClusterRole)

```
Name:         admin
Labels:       kubernetes.io/bootstrapping=rbac-defaults
Annotations:  rbac.authorization.kubernetes.io/autoupdate: true
PolicyRule:
  Resources                                       Non-Resource URLs  Resource Names  Verbs
  ---------                                       -----------------  --------------  -----
  rolebindings.rbac.authorization.k8s.io          []                 []              [create delete deletecollection get list patch update watch]
  roles.rbac.authorization.k8s.io                 []                 []              [create delete deletecollection get list patch update watch]
  configmaps                                      []                 []              [create delete deletecollection patch update get list watch]
  endpoints                                       []                 []              [create delete deletecollection patch update get list watch]
  persistentvolumeclaims                          []                 []              [create delete deletecollection patch update get list watch]
  pods                                            []                 []              [create delete deletecollection patch update get list watch]
  replicationcontrollers/scale                    []                 []              [create delete deletecollection patch update get list watch]
  replicationcontrollers                          []                 []              [create delete deletecollection patch update get list watch]
  services                                        []                 []              [create delete deletecollection patch update get list watch]
  daemonsets.apps                                 []                 []              [create delete deletecollection patch update get list watch]
  deployments.apps/scale                          []                 []              [create delete deletecollection patch update get list watch]
  deployments.apps                                []                 []              [create delete deletecollection patch update get list watch]
  replicasets.apps/scale                          []                 []              [create delete deletecollection patch update get list watch]
  replicasets.apps                                []                 []              [create delete deletecollection patch update get list watch]
  statefulsets.apps/scale                         []                 []              [create delete deletecollection patch update get list watch]
  statefulsets.apps                               []                 []              [create delete deletecollection patch update get list watch]
  horizontalpodautoscalers.autoscaling            []                 []              [create delete deletecollection patch update get list watch]
  cronjobs.batch                                  []                 []              [create delete deletecollection patch update get list watch]
  jobs.batch                                      []                 []              [create delete deletecollection patch update get list watch]
  daemonsets.extensions                           []                 []              [create delete deletecollection patch update get list watch]
  deployments.extensions/scale                    []                 []              [create delete deletecollection patch update get list watch]
  deployments.extensions                          []                 []              [create delete deletecollection patch update get list watch]
  ingresses.extensions                            []                 []              [create delete deletecollection patch update get list watch]
  networkpolicies.extensions                      []                 []              [create delete deletecollection patch update get list watch]
  replicasets.extensions/scale                    []                 []              [create delete deletecollection patch update get list watch]
  replicasets.extensions                          []                 []              [create delete deletecollection patch update get list watch]
  replicationcontrollers.extensions/scale         []                 []              [create delete deletecollection patch update get list watch]
  ingresses.networking.k8s.io                     []                 []              [create delete deletecollection patch update get list watch]
  networkpolicies.networking.k8s.io               []                 []              [create delete deletecollection patch update get list watch]
  poddisruptionbudgets.policy                     []                 []              [create delete deletecollection patch update get list watch]
  deployments.apps/rollback                       []                 []              [create delete deletecollection patch update]
  deployments.extensions/rollback                 []                 []              [create delete deletecollection patch update]
  localsubjectaccessreviews.authorization.k8s.io  []                 []              [create]
  pods/attach                                     []                 []              [get list watch create delete deletecollection patch update]
  pods/exec                                       []                 []              [get list watch create delete deletecollection patch update]
  pods/portforward                                []                 []              [get list watch create delete deletecollection patch update]
  pods/proxy                                      []                 []              [get list watch create delete deletecollection patch update]
  secrets                                         []                 []              [get list watch create delete deletecollection patch update]
  services/proxy                                  []                 []              [get list watch create delete deletecollection patch update]
  bindings                                        []                 []              [get list watch]
  events                                          []                 []              [get list watch]
  limitranges                                     []                 []              [get list watch]
  namespaces/status                               []                 []              [get list watch]
  namespaces                                      []                 []              [get list watch]
  pods/log                                        []                 []              [get list watch]
  pods/status                                     []                 []              [get list watch]
  replicationcontrollers/status                   []                 []              [get list watch]
  resourcequotas/status                           []                 []              [get list watch]
  resourcequotas                                  []                 []              [get list watch]
  controllerrevisions.apps                        []                 []              [get list watch]
  nodes.metrics.k8s.io                            []                 []              [get list watch]
  pods.metrics.k8s.io                             []                 []              [get list watch]
  serviceaccounts                                 []                 []              [impersonate create delete deletecollection patch update get list watch]
```

fission 对应的所有的deployment都是使用fission-svc。

## Executor

### Pool Manager

主要围绕其中比较重要的几点进行探索，如何创建一个pool，如何选择一个GenericPod和如何特化一个Generic Pod以及如何处理一个特化后无用的Generic Pod。

#### Pool Creator

```go
func (gpm *GenericPoolManager) eagerPoolCreator() {
	pollSleep := time.Duration(2 * time.Second)
	for {
		// get list of envs from controller
		envs, err := gpm.fissionClient.Environments(metav1.NamespaceAll).List(metav1.ListOptions{})
		if err != nil {
			if fission.IsNetworkError(err) {
				log.Printf("Encountered network error, retrying: %v", err)
				time.Sleep(5 * time.Second)
				continue
			}
			log.Fatalf("Failed to get environment list: %v", err)
		}

		// Create pools for all envs.  TODO: we should make this a bit less eager, only
		// creating pools for envs that are actually used by functions.  Also we might want
		// to keep these eagerly created pools smaller than the ones created when there are
		// actual function calls.
		for i := range envs.Items {
			env := envs.Items[i]
			// Create pool only if poolsize greater than zero
			if gpm.getEnvPoolsize(&env) > 0 {
				_, err := gpm.GetPool(&envs.Items[i])
				if err != nil {
					log.Printf("eager-create pool failed: %v", err)
				}
			}
		}

		// Clean up pools whose env was deleted
		gpm.CleanupPools(envs.Items)
		time.Sleep(pollSleep)
	}
}

// service
case GET_POOL:
			// just because they are missing in the cache, we end up creating another duplicate pool.
			var err error
			pool, ok := gpm.pools[crd.CacheKey(&req.env.Metadata)]
			if !ok {
				poolsize := gpm.getEnvPoolsize(req.env)
				switch req.env.Spec.AllowedFunctionsPerContainer {
				case fission.AllowedFunctionsPerContainerInfinite:
					poolsize = 1
				}

				// To support backward compatibility, if envs are created in default ns, we go ahead
				// and create pools in fission-function ns as earlier.
				ns := gpm.namespace
				if req.env.Metadata.Namespace != metav1.NamespaceDefault {
					ns = req.env.Metadata.Namespace
				}

				pool, err = MakeGenericPool(
					gpm.fissionClient, gpm.kubernetesClient, req.env, poolsize,
					ns, gpm.namespace, gpm.fsCache, gpm.instanceId, gpm.enableIstio)
				if err != nil {
					req.responseChannel <- &response{error: err}
					continue
				}
				gpm.pools[crd.CacheKey(&req.env.Metadata)] = pool
			}
			req.responseChannel <- &response{pool: pool}
```

该函数本身会作为一个routine存在，首先拿到所有的env的列表，之后遍历所有的env，去向service（主routine）查找相关的pool列表，service中存在cache，如果cache没法读到，会去向k8s api server询问，直接创建相关个数的Pod，Pool的本质就是一个Deployment，我们代码中在`MakeGenericPool`函数中调用`createPool`，里面的主要任务就是创建一个Deployment

#### Generic Pod Create

> K8s automatically creates a new pod after it detects that the actual number of pods managed by the deployment object is smaller than the number of target replicas.

这一条的具体体现如下：

![fission-pod-value](./images/fission-pod-value.png)

新产生的Pod是我们pool里面的pod，上面的Pod是已经特化过的Pod，已经不受Deployment的管辖。

关于Pod的选择，首先通过label找到所有的Pod，之后进行筛选Ready的Pod如果没有，则进行等待，即调用`waitForReadyPod`——主要负责去deployment里面寻找到available，如果没有就继续等待。这个地方的实现存在两重超时，一种是`waitForReayPod`内部的超时处理，一种是`_choosePod`层级的超时处理。我们在此是会想如果有两个人同时调用`_choosePod`会出现race的情况，但是其上游的调用只会是在管道信号传递过来是管道化的。在readyPods里面随机选择一个Pod，之后对该Pod进行relabel。

```go
// choosePod picks a ready pod from the pool and relabels it, waiting if necessary.
// returns the pod API object.
func (gp *GenericPool) choosePod(newLabels map[string]string) (*apiv1.Pod, error) {
	req := &choosePodRequest{
		newLabels:       newLabels,
		responseChannel: make(chan *choosePodResponse),
	}
	gp.requestChannel <- req
	resp := <-req.responseChannel
	return resp.pod, resp.error
}


func (gp *GenericPool) _choosePod(newLabels map[string]string) (*apiv1.Pod, error) {
	startTime := time.Now()
	for {
		// Retries took too long, error out.
		if time.Since(startTime) > gp.podReadyTimeout {
			log.Printf("[%v] Erroring out, timed out", newLabels)
			return nil, errors.New("timeout: waited too long to get a ready pod")
		}

		// Get pods; filter the ones that are ready
		podList, err := gp.kubernetesClient.CoreV1().Pods(gp.namespace).List(
			metav1.ListOptions{
				LabelSelector: labels.Set(
					gp.deployment.Spec.Selector.MatchLabels).AsSelector().String(),
			})
		if err != nil {
			return nil, err
		}
		readyPods := make([]*apiv1.Pod, 0, len(podList.Items))
		for i := range podList.Items {
			pod := podList.Items[i]

			// Ignore not ready pod here
			if !fission.IsReadyPod(&pod) {
				continue
			}

			// add it to the list of ready pods
			readyPods = append(readyPods, &pod)
		}
		log.Printf("[%v] found %v ready pods of %v total", newLabels, len(readyPods), len(podList.Items))

		// If there are no ready pods, wait and retry.
		if len(readyPods) == 0 {
			err = gp.waitForReadyPod()
			if err != nil {
				return nil, err
			}
			continue
		}

		// Pick a ready pod.  For now just choose randomly;
		// ideally we'd care about which node it's running on,
		// and make a good scheduling decision.
		chosenPod := readyPods[rand.Intn(len(readyPods))]

		if gp.env.Spec.AllowedFunctionsPerContainer != fission.AllowedFunctionsPerContainerInfinite {
			// Relabel.  If the pod already got picked and
			// modified, this should fail; in that case just
			// retry.
			chosenPod.ObjectMeta.Labels = newLabels
			log.Printf("relabeling pod: [%v]", chosenPod.ObjectMeta.Name)
			_, err = gp.kubernetesClient.CoreV1().Pods(gp.namespace).Update(chosenPod)
			if err != nil {
				log.Printf("failed to relabel pod [%v]: %v", chosenPod.ObjectMeta.Name, err)
				continue
			}
		}
		log.Printf("Chosen pod: %v (in %v)", chosenPod.ObjectMeta.Name, time.Since(startTime))
		return chosenPod, nil
	}
}

func (gp *GenericPool) waitForReadyPod() error {
	startTime := time.Now()
	for {
		// TODO: for now we just poll; use a watch instead
		depl, err := gp.kubernetesClient.ExtensionsV1beta1().Deployments(gp.namespace).Get(
			gp.deployment.ObjectMeta.Name, metav1.GetOptions{})
		if err != nil {
			err = errors.Wrap(err, fmt.Sprintf(
				"Error waiting for ready pod of deployment %v in namespace %v",
				gp.deployment.ObjectMeta.Name, gp.namespace))
			log.Print(err)
			return err
		}

		gp.deployment = depl
		if gp.deployment.Status.AvailableReplicas > 0 {
			return nil
		}

		if time.Since(startTime) > gp.podReadyTimeout {
			return errors.Errorf(
				"Timeout: waited too long for pod of deployment %v in namespace %v to be ready",
				gp.deployment.ObjectMeta.Name, gp.namespace)
		}
		time.Sleep(1000 * time.Millisecond)
	}
}
```

#### Specialize Pod

这个问题主要由两个子问题，一个是Fetch，一个是Specialize。

Fetch主要是要知道如何获取到用户的Function。

用户的函数被定义在自定义的crd——Package中，我们可以通过package的selfLink下载函数的相关信息。

```go
// Fetch takes FetchRequest and makes the fetch call
// It returns the HTTP code and error if any
func (fetcher *Fetcher) Fetch(ctx context.Context, pkg *fv1.Package, req types.FunctionFetchRequest) (int, error) {
	// check that the requested filename is not an empty string and error out if so
	if len(req.Filename) == 0 {
		e := "fetch request received for an empty file name"
		fetcher.logger.Error(e, zap.Any("request", req))
		return http.StatusBadRequest, errors.New(fmt.Sprintf("%s, request: %v", e, req))
	}

	// verify first if the file already exists.
	if _, err := os.Stat(filepath.Join(fetcher.sharedVolumePath, req.Filename)); err == nil {
		fetcher.logger.Info("requested file already exists at shared volume - skipping fetch",
			zap.String("requested_file", req.Filename),
			zap.String("shared_volume_path", fetcher.sharedVolumePath))
		return http.StatusOK, nil
	}

	tmpFile := req.Filename + ".tmp"
	tmpPath := filepath.Join(fetcher.sharedVolumePath, tmpFile)

	if req.FetchType == types.FETCH_URL {
		// fetch the file and save it to the tmp path
		err := downloadUrl(ctx, fetcher.httpClient, req.Url, tmpPath)
		if err != nil {
			e := "failed to download url"
			fetcher.logger.Error(e, zap.Error(err), zap.String("url", req.Url))
			return http.StatusBadRequest, errors.Wrapf(err, "%s: %s", e, req.Url)
		}
	} else {
		var archive *fv1.Archive
		if req.FetchType == types.FETCH_SOURCE {
			archive = &pkg.Spec.Source
		} else if req.FetchType == types.FETCH_DEPLOYMENT {
			// sometimes, the user may invoke the function even before the source code is built into a deploy pkg.
			// this results in executor sending a fetch request of type FETCH_DEPLOYMENT and since pkg.Spec.Deployment.Url will be empty,
			// we hit this "Get : unsupported protocol scheme "" error.
			// it may be useful to the user if we can send a more meaningful error in such a scenario.
			if pkg.Status.BuildStatus != types.BuildStatusSucceeded && pkg.Status.BuildStatus != types.BuildStatusNone {
				e := fmt.Sprintf("cannot fetch deployment: package build status was not %q", types.BuildStatusSucceeded)
				fetcher.logger.Error(e,
					zap.String("package_name", pkg.Metadata.Name),
					zap.String("package_namespace", pkg.Metadata.Namespace),
					zap.Any("package_build_status", pkg.Status.BuildStatus))
				return http.StatusInternalServerError, errors.New(fmt.Sprintf("%s: pkg %s.%s has a status of %s", e, pkg.Metadata.Name, pkg.Metadata.Namespace, pkg.Status.BuildStatus))
			}
			archive = &pkg.Spec.Deployment
		} else {
			return http.StatusBadRequest, fmt.Errorf("unkonwn fetch type: %v", req.FetchType)
		}

		// get package data as literal or by url
		if len(archive.Literal) > 0 {
			// write pkg.Literal into tmpPath
			err := ioutil.WriteFile(tmpPath, archive.Literal, 0600)
			if err != nil {
				e := "failed to write file"
				fetcher.logger.Error(e, zap.Error(err), zap.String("location", tmpPath))
				return http.StatusInternalServerError, errors.Wrapf(err, "%s %s", e, tmpPath)
			}
		} else {
			// download and verify
			err := downloadUrl(ctx, fetcher.httpClient, archive.URL, tmpPath)
			if err != nil {
				e := "failed to download url"
				fetcher.logger.Error(e, zap.Error(err), zap.String("url", req.Url))
				return http.StatusBadRequest, errors.Wrapf(err, "%s %s", e, req.Url)
			}

			// check file integrity only if checksum is not empty.
			if len(archive.Checksum.Sum) > 0 {
				checksum, err := utils.GetFileChecksum(tmpPath)
				if err != nil {
					e := "failed to get checksum"
					fetcher.logger.Error(e, zap.Error(err))
					return http.StatusBadRequest, errors.Wrap(err, e)
				}
				err = verifyChecksum(checksum, &archive.Checksum)
				if err != nil {
					e := "failed to verify checksum"
					fetcher.logger.Error(e, zap.Error(err))
					return http.StatusBadRequest, errors.Wrap(err, e)
				}
			}
		}
	}

	if archiver.Zip.Match(tmpPath) && !req.KeepArchive {
		// unarchive tmp file to a tmp unarchive path
		tmpUnarchivePath := filepath.Join(fetcher.sharedVolumePath, uuid.NewV4().String())
		err := fetcher.unarchive(tmpPath, tmpUnarchivePath)
		if err != nil {
			fetcher.logger.Error("error unarchiving",
				zap.Error(err),
				zap.String("archive_location", tmpPath),
				zap.String("target_location", tmpUnarchivePath))
			return http.StatusInternalServerError, err
		}

		tmpPath = tmpUnarchivePath
	}

	// move tmp file to requested filename
	renamePath := filepath.Join(fetcher.sharedVolumePath, req.Filename)
	err := fetcher.rename(tmpPath, renamePath)
	if err != nil {
		fetcher.logger.Error("error renaming file",
			zap.Error(err),
			zap.String("original_path", tmpPath),
			zap.String("rename_path", renamePath))
		return http.StatusInternalServerError, err
	}

	fetcher.logger.Info("successfully placed", zap.String("location", renamePath))
	return http.StatusOK, nil
}
```

Fetch里面分为几种方式来进行获取function或对应的可执行文件。

FETCH_URL：

FETCH_DEPLOYMENT：

deployment对应的package在k8s中的显示如下：

```yaml
Name:         txsb-js-yoo7
Namespace:    default
Labels:       <none>
Annotations:  <none>
API Version:  fission.io/v1
Kind:         Package
Metadata:
  Creation Timestamp:  2019-11-04T12:38:37Z
  Generation:          1
  Resource Version:    482287
  Self Link:           /apis/fission.io/v1/namespaces/default/packages/txsb-js-yoo7
  UID:                 54b6bbf3-2b5f-49bb-a3f2-453f8a7ad800
Spec:
  Deployment:
    Checksum:
    Literal:  Cm1vZHVsZS5leHBvcnRzID0gYXN5bmMgZnVuY3Rpb24oY29udGV4dCkgewogICAgcmV0dXJuIHsKICAgICAgICBzdGF0dXM6IDIwMCwKICAgICAgICBib2R5OiAidGFueGlhbyBzYiFcbiIKICAgIH07Cn0K
    Type:     literal
  Environment:
    Name:       nodejs
    Namespace:  default
  Source:
    Checksum:
Status:
  Buildstatus:  succeeded
Events:         <none>
```

这种情况是Literal里面存在记录的情况，将Literal中的信息进行base64解码后即为相应的代码，但这仅仅限于代码非常少且代码组织形式很简单，没有多文件的组织形式。

另一种方式是给予相关的文件的url，直接从url download下内容。

Specialize部分：

主要是调用端口在8888上/specialize接口，会调用fetcher中的specializePod的接口

```go
func (fetcher *Fetcher) SpecializePod(ctx context.Context, fetchReq types.FunctionFetchRequest, loadReq types.FunctionLoadRequest) error {
	startTime := time.Now()
	defer func() {
		elapsed := time.Since(startTime)
		fetcher.logger.Info("specialize request done", zap.Duration("elapsed_time", elapsed))
	}()

	pkg, err := fetcher.getPkgInformation(fetchReq)
	if err != nil {
		return errors.Wrap(err, "error getting package information")
	}

	_, err = fetcher.Fetch(ctx, pkg, fetchReq)
	if err != nil {
		return errors.Wrap(err, "error fetching deploy package")
	}

	_, err = fetcher.FetchSecretsAndCfgMaps(fetchReq.Secrets, fetchReq.ConfigMaps)
	if err != nil {
		return errors.Wrap(err, "error fetching secrets/configs")
	}

	// Specialize the pod

	maxRetries := 30
	var contentType string
	var specializeURL string
	var reader *bytes.Reader

	loadPayload, err := json.Marshal(loadReq)
	if err != nil {
		return errors.Wrap(err, "error encoding load request")
	}

	// Instead of using "localhost", here we use "127.0.0.1" for
	// inter-pod communication to prevent wrongly record returned from DNS.

	if loadReq.EnvVersion >= 2 {
		contentType = "application/json"
		specializeURL = "http://127.0.0.1:8888/v2/specialize"
		reader = bytes.NewReader(loadPayload)
		fetcher.logger.Info("calling environment v2 specialization endpoint")
	} else {
		contentType = "text/plain"
		specializeURL = "http://127.0.0.1:8888/specialize"
		reader = bytes.NewReader([]byte{})
		fetcher.logger.Info("calling environment v1 specialization endpoint")
	}

	for i := 0; i < maxRetries; i++ {
		resp, err := http.Post(specializeURL, contentType, reader)
		if err == nil && resp.StatusCode < 300 {
			// Success
			resp.Body.Close()
			return nil
		}

		netErr := network.Adapter(err)
		// Only retry for the specific case of a connection error.
		if netErr != nil && (netErr.IsConnRefusedError() || netErr.IsDialError()) {
			if i < maxRetries-1 {
				time.Sleep(500 * time.Duration(2*i) * time.Millisecond)
				fetcher.logger.Error("error connecting to function environment pod for specialization request, retrying", zap.Error(netErr))
				continue
			}
		}

		// for 4xx, 5xx
		if err == nil {
			err = ferror.MakeErrorFromHTTP(resp)
		}

		return errors.Wrap(err, "error specializing function pod")
	}

	return errors.Wrapf(err, "error specializing function pod after %v times", maxRetries)
}
```

`/specialize`的具体实现是跟环境有关的，我们在fission的具体repo里面可以看到语言相关的实现，即，fission里面的env启动的时候的指令其实是运行了一个server，比如nodejs环境的[代码](https://github.com/fission/fission/blob/ef8289ff73fa3503466f76e0902e11aa7b5792c7/environments/nodejs/server.js)

