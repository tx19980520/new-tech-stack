FROM lucasmundim/nginx-lua-push-stream
COPY default.conf config/nginx.conf
RUN mkdir /usr/share/nginx-cache
ENTRYPOINT ["nginx"]
