package main

import (
	"fmt"
	"net/http"
)

func handle(res http.ResponseWriter, req *http.Request) {
	var body []byte
	_, err := req.Body.Read(body)
	if err != nil {
		fmt.Println(err.Error())
		res.WriteHeader(500)
		return
	}
	_, err = res.Write(body)
	if err != nil {
		fmt.Println(err.Error())
		res.WriteHeader(500)
	}
}