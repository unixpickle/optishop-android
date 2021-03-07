package liboptishop

import (
	"sync"

	"github.com/unixpickle/optishop-server/optishop/db"
	"github.com/unixpickle/optishop-server/serverapi"
)

var settingUp bool
var setupComplete bool
var setupError error
var setupLock sync.Mutex

func StartSetup(assetsDir, storageDir string) {
	setupLock.Lock()
	defer setupLock.Unlock()

	if settingUp || setupComplete {
		return
	}
	settingUp = true
	setupError = nil

	go func() {
		dbInstance, err := db.NewLocalDB(storageDir)
		if err != nil {
			finishSetup(err)
			return
		}

		sources, err := serverapi.LoadStoreSources()
		if err != nil {
			finishSetup(err)
			return
		}

		server := &serverapi.Server{
			AssetDir:   assetsDir,
			NumProxies: 0,
			LocalMode:  true,
			DB:         dbInstance,
			Sources:    sources,
			StoreCache: serverapi.NewStoreCache(sources),
		}
		server.AddRoutes()
		finishSetup(nil)
	}()
}

func finishSetup(err error) {
	setupLock.Lock()
	defer setupLock.Unlock()
	settingUp = false
	if err != nil {
		setupError = err
	} else {
		setupComplete = true
	}
}

func SetupComplete() bool {
	setupLock.Lock()
	defer setupLock.Unlock()
	return setupComplete
}

func SetupError() string {
	setupLock.Lock()
	defer setupLock.Unlock()
	if setupError == nil {
		return ""
	}
	return setupError.Error()
}
