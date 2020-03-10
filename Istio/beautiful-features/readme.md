# Beautiful Features

因为上课（对，上课他就是没时间看）没有再继续跟进相关Istio进几次的release文档，不知道最近支持了一些什么新功能（要面试的嘛），主要按照release note的相关内容来进行了解。如果需要更加深入的了解，会单开一个专题进行论述和实践。

## locality load balancing

实质上这是一个配合kubernetes进行的相关工作，主要定义了`(Region, Zone, Sub-zone)`的三元组概念，通过多出的地域信息，来帮助负载均衡的进行，从而降低延迟。这里要强调的是，我们只能降低集群内部Pod与Pod之间的调用延迟，对于用户的入口——ingress——这个是用户自我进行选择的，无权进行干涉（域名指定）。官网给出的相关例子主要考虑的是在某区域服务出现故障的情况下，另一区域能否暂时的进行流量的接管，或者为了防止该地区的问题影响到其他的地区，是否会设置相应的隔离设置。多出地域的标签区分，确实对于整体的管控有着良好的效果。

另一方面的问题，Istio如何能够给予kubernetes相应的反馈呢，即如果某地区的访问增加，deployment应当是某地区的相关服务进行扩容，应当以一个什么样的方式来进行呢，我们肯定希望扩容是向需求量增加的地方进行扩容。似乎kubernetes这边没有有关于这个方面上的相关事宜可以进行工作。

## Health Check

这其实不算是一个feature，只能算是一个fix，原因就在于在使用Istio管理网络的情况下，探针的探测是kubelet进行相应的操作，而kubelet在Istio默认开启mTLS的情况下，无法与端口进行正常的交互，因此会出现错误，解决方案主要是两种——加上适当的annotation，告诉sidecar这里有相应的探针检查（那你咋知道这是探针检查还是外部访问呢，非常的疑惑），我们来查阅相关的代码，进行解惑：

```go
rewrite := ShouldRewriteAppHTTPProbers(pod.Annotations, sic)
	addAppProberCmd := func() {
		if !rewrite {
			return
		}
		sidecar := FindSidecar(sic.Containers)
		if sidecar == nil {
			log.Errorf("sidecar not found in the template, skip addAppProberCmd")
			return
		}
		// We don't have to escape json encoding here when using golang libraries.
		if prober := DumpAppProbers(&pod.Spec); prober != "" {
			sidecar.Env = append(sidecar.Env, corev1.EnvVar{Name: status.KubeAppProberEnvName, Value: prober})
		}
	}
	addAppProberCmd()
```

上文可以看到如果存在注解，我们会给sidecar加上相应的环境变量，内容就是探针的相关信息。

```go
func (s *Server) handleAppProbe(w http.ResponseWriter, req *http.Request) {
	// Validate the request first.
	path := req.URL.Path
	if !strings.HasPrefix(path, "/") {
		path = "/" + req.URL.Path
	}
	prober, exists := s.appKubeProbers[path]
	if !exists {
		log.Errorf("Prober does not exists url %v", path)
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(fmt.Sprintf("app prober config does not exists for %v", path)))
		return
	}

	// Construct a request sent to the application.
	httpClient := &http.Client{
		Timeout: time.Duration(prober.TimeoutSeconds) * time.Second,
		// We skip the verification since kubelet skips the verification for HTTPS prober as well
		// https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/#configure-probes
		Transport: &http.Transport{
			TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
		},
	}
	var url string
	if prober.HTTPGet.Scheme == corev1.URISchemeHTTPS {
		url = fmt.Sprintf("https://localhost:%v%s", prober.HTTPGet.Port.IntValue(), prober.HTTPGet.Path)
	} else {
		url = fmt.Sprintf("http://localhost:%v%s", prober.HTTPGet.Port.IntValue(), prober.HTTPGet.Path)
	}
	appReq, err := http.NewRequest("GET", url, nil)
	if err != nil {
		log.Errorf("Failed to create request to probe app %v, original url %v", err, path)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Forward incoming headers to the application.
	for name, values := range req.Header {
		newValues := make([]string, len(values))
		copy(newValues, values)
		appReq.Header[name] = newValues
	}

	for _, h := range prober.HTTPGet.HTTPHeaders {
		if h.Name == "Host" || h.Name == ":authority" {
			// Probe has specific host header override; honor it
			appReq.Host = h.Value
			break
		}
	}

	// Send the request.
	response, err := httpClient.Do(appReq)
	if err != nil {
		log.Errorf("Request to probe app failed: %v, original URL path = %v\napp URL path = %v", err, path, prober.HTTPGet.Path)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	defer response.Body.Close()

	// We only write the status code to the response.
	w.WriteHeader(response.StatusCode)
}
```

我们看到这里存在一个转发，但是这个接口本身会prefix一个/app-health/，我个人认为，这里存在对我们的探针进行覆写这样的一个情况。

另外一方面，也可以使用分离端口的方式，对外服务的暴露使用一个端口，进行探针测试服务用一个端口，则Istio不会对其进行相应的管理。

