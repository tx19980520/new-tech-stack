# base image
FROM centos

# MAINTAINER
MAINTAINER ty0207 ty0207@sjtu.edu.cn

# put nginx-1.12.2.tar.gz into /usr/local/src and unpack nginx
ADD http://nginx.org/download/nginx-1.14.0.tar.gz .

#RUN 执行以下命令 
RUN yum install -y pcre-devel wget net-tools gcc zlib zlib-devel make openssl-devel
RUN useradd -M -s /sbin/nologin nginx
RUN tar -zxvf nginx-1.14.0.tar.gz
RUN mkdir -p /usr/local/nginx
RUN cd nginx-1.14.0 && ./configure --prefix=/usr/local/nginx --user=nginx --group=nginx --with-http_stub_status_module && make && make install
RUN ln -s /usr/local/nginx/sbin/* /usr/local/sbin/
 
#EXPOSE 映射端口
EXPOSE 80
 
#CMD 运行以下命令
CMD ["nginx"]