package com.isoftstone.dispatch.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.alibaba.fastjson.JSON;
import com.isoftstone.dispatch.consts.DispatchConstant;
import com.isoftstone.dispatch.vo.DispatchVo;

public class RedisUtils {

	private static final Log LOG = LogFactory.getLog(RedisUtils.class);

	private static JedisPool pool = null;

	public static JedisPool getPool() {
		if (pool == null) {
			JedisPoolConfig config = new JedisPoolConfig();

			config.setMaxIdle(500);

			config.setMaxIdle(5);
			config.setMaxWaitMillis(1000 * 100);

			config.setTestOnBorrow(true);

			String ip = DispatchConstant.REDIS_IP;
			if (Config.getValue("template.redis.ip") != null) {
				ip = Config.getValue("template.redis.ip");
			}

			int port = DispatchConstant.REDIS_PORT;
			if (Config.getValue("template.redis.port") != null) {
				port = Integer.parseInt(Config.getValue("template.redis.port"));
			}

			pool = new JedisPool(config, ip, port);
		}
		return pool;
	}

	public static void returnResource(JedisPool pool, Jedis redis) {
		if (redis != null) {
			pool.returnResource(redis);
		}
	}

	public static DispatchVo getDispatchResult(String guid, int dbindex) {
		return (DispatchVo) getResult(DispatchVo.class, guid, dbindex);
	}

	public static Object getResult(Class clazz, String guid, int dbindex) {
		JedisPool pool = null;
		Jedis jedis = null;
		try {
			pool = getPool();
			jedis = pool.getResource();
			jedis.select(dbindex);
			String json = jedis.get(guid);
			if (json != null)
				return JSON.parseObject(json, clazz);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			LOG.error("", e);
		} finally {
			returnResource(pool, jedis);
		}
		return null;
	}

	/*
	 * 获取所有符合条件的结果List.
	 */
	public static List<String> getResultList(String guid, int dbindex) {
		JedisPool pool = null;
		Jedis jedis = null;
		try {
			pool = getPool();
			jedis = pool.getResource();
			jedis.select(dbindex);
			Set<String> set = jedis.keys(guid);
			List<String> resultList = new ArrayList<String>();
			resultList.addAll(set);
			return resultList;
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			LOG.error("", e);
		} finally {
			returnResource(pool, jedis);
		}
		return null;
	}

	public static void setResult(Object object, String guid, int dbindex) {
		JedisPool pool = null;
		Jedis jedis = null;
		try {
			StringBuilder str = new StringBuilder();
			str.append(JSON.toJSONString(object));
			pool = getPool();
			jedis = pool.getResource();
			jedis.select(dbindex);
			jedis.set(guid, str.toString());
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			LOG.error("", e);
		} finally {
			returnResource(pool, jedis);
		}
	}

	public static long remove(String guid, int dbindex) {
		JedisPool pool = null;
		Jedis jedis = null;
		try {
			pool = getPool();
			jedis = pool.getResource();
			jedis.select(dbindex);
			return jedis.del(guid);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			LOG.error("", e);
		} finally {
			returnResource(pool, jedis);
		}
		return -1;
	}

	public static boolean contains(String guid, int dbindex) {
		JedisPool pool = null;
		Jedis jedis = null;
		boolean flag = false;
		try {
			pool = getPool();
			jedis = pool.getResource();
			jedis.select(dbindex);
			flag = jedis.exists(guid);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			LOG.error("", e);
		} finally {
			returnResource(pool, jedis);
		}
		return flag;
	}
	
	public static List<DispatchVo> getDispatchListResult(List<String> redisKeys,int dbIndex){
        JedisPool pool = null;
        Jedis jedis = null;
        List<DispatchVo> result= new ArrayList<>();

        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(dbIndex);
            List<String> json = jedis.mget(redisKeys.toArray(new String[0]));
            if (json != null) {
                for(String js : json)
                    result.add(JSON.parseObject(js, DispatchVo.class));
            }
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            LOG.error("get dispatch result from redis failed", e);
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
        return result;
    }

}
