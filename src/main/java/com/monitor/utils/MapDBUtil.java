package com.monitor.utils;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.ConcurrentMap;

@Deprecated
@Component
public class MapDBUtil {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${db.file}")
    private String dbpath;

    public enum TABLE {
        ec2InstanceStatus, instanceCluster, codePipelines;
    }

    private static final Object lock = new Object();

    public void init() {
        File file = new File(dbpath);
        if (file.exists()) {
            logger.warn(String.valueOf(file.delete()));
        }
    }

    public void save(ConcurrentMap<String, String> map, TABLE table) {
        synchronized (lock) {
            try (DB db = DBMaker.fileDB(dbpath).fileMmapEnable().make(); HTreeMap<String, String> hTreeMap = db.hashMap(table.name(), Serializer.STRING, Serializer.STRING).createOrOpen()) {
                hTreeMap.putAll(map);
                db.commit();
            }
        }
    }

    public ConcurrentMap<String, String> read(TABLE table) {
        synchronized (lock) {
            try (DB db = DBMaker.fileDB(dbpath).fileMmapEnable().make(); HTreeMap<String, String> hTreeMap = db.hashMap(table.name(), Serializer.STRING, Serializer.STRING).createOrOpen()) {
                return hTreeMap;
            }
        }
    }
}
