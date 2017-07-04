package xyz.derkades.serverselectorx.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Cache {
	
	private static final Map<String, CacheObject> CACHE_OBJECT_MAP = new HashMap<>();
	
	/**
	 * @param identifier
	 * @param object
	 * @param timeout In seconds
	 */
	public static void addCachedObject(String identifier, Object object, long timeout){
		removeCachedObject(identifier); //Remove existing cached object
		
		if (timeout < 0){
			timeout = Long.MAX_VALUE;
		}
		
		CacheObject cachedObject = new CacheObject(object, timeout);
		CACHE_OBJECT_MAP.put(identifier, cachedObject);
	}
	
	/**
	 * Caches object for an hour. <br>
	 * See also: {@link #addCachedObject(String, Object, long)}
	 */
	public static void addCachedObject(String identifier, Object object){
		addCachedObject(identifier, object, 3600);
	}
	
	public static Object getCachedObject(String identifier){
		CacheObject cache = CACHE_OBJECT_MAP.get(identifier);
		
		if (cache == null) return null;
		
		if (System.currentTimeMillis() - cache.timeCreated > cache.timeout){
			CACHE_OBJECT_MAP.remove(identifier);
			return null;
		} else {
			return cache.object;
		}
	}
	
	public static void removeCachedObject(String identifier){
		CACHE_OBJECT_MAP.remove(identifier);
	}
	
	/**
	 * @return Number of objects removed from cache
	 */
	public static int cleanCache(){
		List<String> removeQueue = new ArrayList<>();
		
		for (Entry<String, CacheObject> entry : CACHE_OBJECT_MAP.entrySet()){
			CacheObject cache = entry.getValue();
			if (System.currentTimeMillis() - cache.timeCreated > cache.timeout){
				removeQueue.add(entry.getKey());
			}
		}
		
		for (String identifier : removeQueue){
			removeCachedObject(identifier);
		}
		
		return removeQueue.size();
	}
	
	public static int size(){
		return CACHE_OBJECT_MAP.size();
	}

}
