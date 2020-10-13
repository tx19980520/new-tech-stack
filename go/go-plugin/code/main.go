package main

import (
	"context"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"plugin"
)

func loadPlugin(codePath string, entrypoint string) (http.HandlerFunc, error) {
	info, err := os.Stat(codePath)
	if err != nil {
		return nil, fmt.Errorf("error checking plugin path: %v", err)
	}
	if info.IsDir() {
		files, err := ioutil.ReadDir(codePath)
		if err != nil {
			return nil, fmt.Errorf("error reading directory: %v", err)
		}
		if len(files) == 0 {
			return nil, fmt.Errorf("no files to load: %v", codePath)
		}
		fi := files[0]
		codePath = filepath.Join(codePath, fi.Name())
	}

	log.Printf("loading plugin from %v", codePath)
	p, err := plugin.Open(codePath)
	if err != nil {
		return nil, fmt.Errorf("error loading plugin: %v", err)
	}
	sym, err := p.Lookup(entrypoint)
	if err != nil {
		return nil, fmt.Errorf("entry point not found: %v", err)
	}

	switch h := sym.(type) {
	case *http.Handler:
		return (*h).ServeHTTP, nil
	case *http.HandlerFunc:
		return *h, nil
	case func(http.ResponseWriter, *http.Request):
		return h, nil
	case func(context.Context, http.ResponseWriter, *http.Request):
		return func(w http.ResponseWriter, r *http.Request) {
			c := context.Background()
			h(c, w, r)
		}, nil
	default:
		panic("Entry point not found: bad type")
	}
}

func main() {
	userFunc, err := loadPlugin("./plugins/example-plugin.so", "Handle")
	if err != nil {
		panic(err)
	}
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		userFunc(w, r)
	})
	http.ListenAndServe(":8888", nil)
}
