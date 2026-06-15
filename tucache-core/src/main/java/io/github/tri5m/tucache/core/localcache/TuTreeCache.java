package io.github.tri5m.tucache.core.localcache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 一个基于 ConcurrentHashMap 的本地缓存，可以按照通配前缀删除
 * 在树的层级越低越近于 ConcurrentHashMap 的性能
 *
 * @title: TuTreeCache
 * @author: trifolium.wang
 * @date: 2023/12/8
 * @modified:
 */
@Slf4j
@Deprecated
public class TuTreeCache {

    public static final long NOT_EXPIRE = -1;
    private final CacheTable cacheTable;

    /**
     * 缓存键分隔符
     */
    private final String DELIMITER;

    public TuTreeCache() {
        this(":");
    }

    public TuTreeCache(String delimiter) {
        this.DELIMITER = delimiter;
        cacheTable = new CacheTable();
        init();
    }

    private void init() {

        // 启动一个全局主动清理过期缓存的线程
        // 实际上在缓存使用的过程中沿路径的过期缓存都会被清理掉，
        // 但有一些路径可能或者永久不会被扫描到，
        // 这里的全局扫描就是解决这个内存泄漏问题的。
        // 递归扫描进行清理
        Thread scanRemove = new Thread(() -> {
            long sleepMillisSecond = 5000L;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepMillisSecond);
                    // 递归扫描进行清理
                    log.trace("tu_tree_cache clear expired cache data.");
                    scanRemove(cacheTable);
                } catch (InterruptedException e) {
                    log.trace(e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            }
        });
        scanRemove.setDaemon(true);
        scanRemove.setName("tu_tree_cache-local-scan-clean");
        scanRemove.start();
    }

    /**
     * 从缓存表中查找缓存Node
     */
    public CacheNode searchNode(String key) {
        if (!StringUtils.hasLength(key)) {
            return null;
        }
        String[] keys = key.split(DELIMITER, -1);
        CacheTable currentHierarchy = cacheTable;
        CacheNode currentNode;
        for (int i = 0; i < keys.length; i++) {
            String currentKey = keys[i];
            if (currentHierarchy == null) {

                return null;
            }
            currentNode = currentHierarchy.get(currentKey);
            if (currentNode == null) {

                return null;
            }

            // 过期的情况下,清理当前路径下的节点
            if (currentNode.isExpired()) {
                if (currentNode.getChild() == null || currentNode.getChild().isEmpty()) {
                    // 过期的缓存
                    currentHierarchy.remove(currentKey);
                    return null;
                }
                currentNode.setObj(null);
                currentNode.setExpire(-1);
                if (i + 1 == keys.length) {
                    // 直接返回，少一步get
                    return null;
                }
            }

            // 如果是叶子节点
            if (i + 1 == keys.length) {

                return currentNode;
            }
            currentHierarchy = currentNode.getChild();
        }
        return null;
    }

    /**
     * 向节点中插入数据
     *
     * @param timeout 过期时长 单位毫秒
     */
    public CacheNode putNode(String key, Object obj, Long timeout) {
        if (!StringUtils.hasLength(key)) {

            throw new IllegalArgumentException("add local cache key is null.");
        } else {
            String[] keys = key.split(DELIMITER, -1);
            CacheTable currentHierarchy = cacheTable;
            CacheNode currentNode = null;
            for (int i = 0; i < keys.length; i++) {
                String currentKey = keys[i];
                currentNode = currentHierarchy.get(currentKey);
                if (currentNode == null) {
                    // 不存在则创建新的节点
                    currentNode = new CacheNode(-1, null, null);
                    currentHierarchy.put(currentKey, currentNode);
                } else {
                    // 存在的话则清除路径上的过期数据
                    // 过期的情况下,清理当前路径下的节点数据，但是其他的保留
                    if (currentNode.isExpired()) {
                        currentNode.setObj(null);
                        currentNode.setExpire(-1);
                    }
                }

                if (i + 1 == keys.length) {
                    // 如果是叶子节点，则存储数据节点
                    currentNode = new CacheNode(timeout == null ? -1 : (System.currentTimeMillis() + timeout),
                            currentNode.getChild(), obj);
                    currentHierarchy.put(currentKey, currentNode);

                    return currentNode;
                } else {
                    // 否则直接拿到孩子节点并继续下一轮查询
                    currentHierarchy = currentNode.getChild();
                    if (currentHierarchy == null) {
                        CacheTable child = new CacheTable();
                        currentNode.setChild(child);
                        currentHierarchy = child;
                    }
                }

            }

            return currentNode;
        }
    }

    /**
     * 移除某个指定的缓存
     */
    public void remove(String key) {
        if (!StringUtils.hasLength(key)) {
            return;
        }
        String[] keys = key.split(DELIMITER, -1);
        CacheTable currentHierarchy = cacheTable;
        CacheNode currentNode;
        for (int i = 0; i < keys.length; i++) {
            String currentKey = keys[i];
            if (currentHierarchy == null) {
                return;
            }
            currentNode = currentHierarchy.get(currentKey);
            if (currentNode == null) {

                return;
            }
            // 如果是叶子节点
            if (i + 1 == keys.length) {
                if (currentNode.getChild() == null || currentNode.getChild().isEmpty()) {
                    currentHierarchy.remove(currentKey);
                    return;
                }
                currentNode.setObj(null);
                currentNode.setExpire(-1);
            } else {
                // 如果是中间节点且已过期，清理节点
                if (currentNode.isExpired()) {
                    // 如果是中间节点
                    if (currentNode.getChild() == null || currentNode.getChild().isEmpty()) {
                        currentHierarchy.remove(currentKey);
                        return;
                    } else {
                        currentNode.setObj(null);
                        currentNode.setExpire(-1);
                    }
                }
            }
            currentHierarchy = currentNode.getChild();
        }
    }

    /**
     * 移除缓存。默认删除精确key，包含通配符"*"时按前缀删除。
     */
    public void removeKeys(String keyPattern) {
        if (!StringUtils.hasLength(keyPattern)) {
            return;
        }

        if (!keyPattern.endsWith("*")) {
            remove(keyPattern);
            return;
        }

        String keyPrefix = trimTrailingWildcard(keyPattern);
        if (!StringUtils.hasLength(keyPrefix)) {
            cacheTable.clear();
            return;
        }

        removeByPrefix(cacheTable, null, keyPrefix);
    }

    private void removeByPrefix(CacheTable cacheTable, String path, String keyPrefix) {
        if (cacheTable == null || cacheTable.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<String, CacheNode>> iterator = cacheTable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheNode> entry = iterator.next();
            String currentPath = path == null ? entry.getKey() : path + DELIMITER + entry.getKey();
            if (currentPath.startsWith(keyPrefix)) {
                iterator.remove();
                continue;
            }

            removeByPrefix(entry.getValue().getChild(), currentPath, keyPrefix);
        }
    }

    private String trimTrailingWildcard(String key) {
        String result = key;
        while (result.endsWith("*")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 递归扫描过期的缓存并清除。
     */
    private void scanRemove(CacheTable ct) {
        if (ct == null || ct.isEmpty()) return;
        Iterator<Map.Entry<String, CacheNode>> iterator = ct.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheNode> entry = iterator.next();
            scanRemove(entry.getValue().getChild());
            if (entry.getValue().isExpired()) {
                if (entry.getValue().getChild() == null || entry.getValue().getChild().isEmpty()) {
                    iterator.remove();
                } else {
                    entry.getValue().setObj(null);
                    entry.getValue().setExpire(-1);
                }
            }
        }
    }


    /**
     * 缓存数据节点
     */
    @Data
    @AllArgsConstructor
    public static class CacheNode {
        private long expire;
        private CacheTable child;
        private Object obj;

        /**
         * 是否过期
         */
        public boolean isExpired() {

            return this.expire >= 0 && this.expire <= System.currentTimeMillis();
        }
    }

    /**
     * 缓存表，就是一个ConcurrentHashMap
     */
    static class CacheTable extends ConcurrentHashMap<String, CacheNode> {

    }
}
