# MongoDB

mongodb的集群安装主要分为两个部分，第一个部分是statfulset进行mongo部署，第二个是在pod中注入mongo-sidecar来保证replica set的正常运作。我们按照某些博客的教程进行配置，发现一些不妥之处。

首先是没有强调mongo-sidecar的相关设置，首先是mongo-sidecar自身的设置，其次是mongo-sidecar需要使用到kubernetes中的用户，在其他的ns中创建该项服务的话会存在没有权限，导致在mongo-sidecar中呈现403的情况。

```js
{  
message:
   { 
     kind: 'Status',
     apiVersion: 'v1',
     metadata: {},
     status: 'Failure',
     message:
      'pods is forbidden: User "system:serviceaccount:mongo:default" cannot list resource "pods" in API group "" in the namespace "mongo"',
     reason: 'Forbidden',
     details: { kind: 'pods' },
     code: 403 
   },
  statusCode: 403 
}
```

我们在初始化statefulset的时候显示的给mongo-sidecar添加了对应的用户，在配置文件中有我们对用户的相关设置。