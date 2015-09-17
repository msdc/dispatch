package com.isoftstone.dispatch.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.isoftstone.dispatch.vo.Runmanager;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ShellUtils {

	public static final Log LOG = LogFactory.getLog(ShellUtils.class);

	/**
	 * 连接到指定的IP
	 * 
	 * @throws JSchException
	 */
	public Session connect(String user, String passwd, String host) throws JSchException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(user, host, 22);
		session.setPassword(passwd);

		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);

		session.connect();
		return session;
	}

	/**
	 * 0.0 Description:连接到指定的IP
	 * 
	 * CreateTime: 2014年6月3日 上午9:04:43
	 * 
	 * @param runmanager
	 * @throws Exception
	 */
	public Session connect(Runmanager runmanager) throws Exception {
		JSch jsch = new JSch();

		String keyPath = runmanager.getKeyPath();
		String passPhrase = runmanager.getPassPhrase();
		Integer port = runmanager.getPort();
		String username = runmanager.getUsername();
		String host = runmanager.getHostIp();
		String password = runmanager.getPassword();

		// 设置密钥和密码
		if (keyPath != null && !"".equals(keyPath)) {
			if (passPhrase != null && !"".equals(passPhrase)) {
				// 设置带口令的密钥
				jsch.addIdentity(keyPath, passPhrase);
			} else {
				// 设置不带口令的密钥
				// jsch.addIdentity(keyPath);
			}
		}
		Session session;

		if (port <= 0) {
			// 连接服务器，采用默 认端口
			session = jsch.getSession(username, host);
		} else {
			// 采用指定的端口连接服务器
			session = jsch.getSession(username, host, port);
		}

		// 如果服务器连接不上，则抛出异常
		if (session == null) {
			throw new Exception("session is null");
		}

		// 设置登陆主机的密码
		session.setPassword(password);

		java.util.Properties config = new java.util.Properties();
		// 设置第一次登陆的时候提示，可选值：(ask | yes | no)
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);

		// 设置登陆超时时间
		session.connect(3000);
		return session;
	}

	/**
	 * 0.0 Description:判断字符串是否为空
	 * 
	 * CreateTime: 2014年5月29日 下午1:02:14
	 * 
	 * @param str
	 * @return
	 */
	public boolean isEmpty(String str) {
		boolean flag = true;
		if (!"".equals(str) && str != null) {
			flag = false;
		}

		return flag;
	}

	/**
	 * 0.0 Description:执行相关的命令
	 * 
	 * CreateTime: 2014年6月3日 上午9:05:45
	 * 
	 * @param runmanager
	 * @throws Exception
	 */
	public void execCmd(Runmanager runmanager) {
		Session session = null;
		try {
			session = connect(runmanager);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

		String command = runmanager.getCommand();
		BufferedReader reader = null;
		Channel channel = null;

		try {
			if (command != null) {
				channel = session.openChannel("exec");
				((ChannelExec) channel).setCommand(command);

				channel.setInputStream(null);
				((ChannelExec) channel).setErrStream(System.err);

				channel.connect();
				InputStream in = channel.getInputStream();
				reader = new BufferedReader(new InputStreamReader(in));

				// 获取命令执行的结果
				// String buf = null;
				// while ((buf = reader.readLine()) != null) {
				// LOG.info(buf);
				// }
				byte[] tmp = new byte[1024];
				while (true) {
					while (in.available() > 0) {
						int i = in.read(tmp, 0, 1024);
						if (i < 0) {
							break;
						}
						LOG.info(new String(tmp, 0, i));
					}
					if (channel.isClosed()) {
						if (in.available() > 0) {
							continue;
						}
						LOG.info("exit-status: " + channel.getExitStatus());
						break;
					}
					try {
						Thread.sleep(1000);
					} catch (Exception ee) {
						LOG.error("", ee);
					}
				}
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		} catch (JSchException e) {
			LOG.error(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
			channel.disconnect();
			session.disconnect();
		}
	}

	/**
	 * 执行相关的命令
	 * 
	 * @throws JSchException
	 */
//	public void execCmd(String command, String user, String passwd, String host) throws Exception {
//		Session session = connect(user, passwd, host);
//
//		BufferedReader reader = null;
//		Channel channel = null;
//
//		try {
//			if (command != null) {
//				channel = session.openChannel("exec");
//				((ChannelExec) channel).setCommand(command);
//
//				channel.setInputStream(null);
//				((ChannelExec) channel).setErrStream(System.err);
//
//				channel.connect();
//				InputStream in = channel.getInputStream();
//				reader = new BufferedReader(new InputStreamReader(in));
//
//				// 获取命令执行的结果
//				if (in.available() > 0) {
//					byte[] data = new byte[in.available()];
//					int nLen = in.read(data);
//
//					if (nLen < 0) {
//						throw new Exception("network error.");
//					}
//
//					// 转换输出结果并打印出来
//					String temp = new String(data, 0, nLen, "utf-8");
//					LOG.info(temp);
//				}
//
//				String buf = null;
//				while ((buf = reader.readLine()) != null) {
//					LOG.info(buf);
//				}
//			}
//		} catch (IOException e) {
//			LOG.error(e.getMessage());
//		} catch (JSchException e) {
//			LOG.error(e.getMessage());
//		} finally {
//			try {
//				reader.close();
//				channel.disconnect();
//				session.disconnect();
//			} catch (IOException e) {
//				LOG.error(e.getMessage());
//			}
//		}
//	}
//
//	/**
//	 * 利用JSch包实现远程主机SHELL命令执行
//	 * 
//	 * @param ip
//	 *            主机IP
//	 * @param user
//	 *            主机登陆用户名
//	 * @param psw
//	 *            主机登陆密码
//	 * @param port
//	 *            主机ssh2登陆端口，如果取默认值，传-1
//	 * @param privateKey
//	 *            密钥文件路径
//	 * @param passphrase
//	 *            密钥的密码
//	 */
//	public static void sshShell(String ip, String user, String psw, int port, String privateKey, String passphrase) throws Exception {
//		Session session = null;
//		Channel channel = null;
//
//		JSch jsch = new JSch();
//
//		// 设置密钥和密码
//		if (privateKey != null && !"".equals(privateKey)) {
//			if (passphrase != null && !"".equals(passphrase)) {
//				// 设置带口令的密钥
//				jsch.addIdentity(privateKey, passphrase);
//			} else {
//				// 设置不带口令的密钥
//				jsch.addIdentity(privateKey);
//			}
//		}
//
//		if (port <= 0) {
//			// 连接服务器，采用默认端口
//			session = jsch.getSession(user, ip);
//		} else {
//			// 采用指定的端口连接服务器
//			session = jsch.getSession(user, ip, port);
//		}
//
//		// 如果服务器连接不上，则抛出异常
//		if (session == null) {
//			throw new Exception("session is null");
//		}
//
//		// 设置登陆主机的密码
//		session.setPassword(psw);// 设置密码
//		// 设置第一次登陆的时候提示，可选值：(ask | yes | no)
//		session.setConfig("StrictHostKeyChecking", "no");
//		// 设置登陆超时时间
//		session.connect(30000);
//
//		try {
//			// 创建sftp通信通道
//			channel = (Channel) session.openChannel("shell");
//			channel.connect(1000);
//
//			// 获取输入流和输出流
//			InputStream instream = channel.getInputStream();
//			OutputStream outstream = channel.getOutputStream();
//
//			// 发送需要执行的SHELL命令，需要用\n结尾，表示回车
//			String shellCommand = "ls \n";
//			outstream.write(shellCommand.getBytes());
//			outstream.flush();
//
//			// 获取命令执行的结果
//			if (instream.available() > 0) {
//				byte[] data = new byte[instream.available()];
//				int nLen = instream.read(data);
//
//				if (nLen < 0) {
//					throw new Exception("network error.");
//				}
//
//				// 转换输出结果并打印出来
//				String temp = new String(data, 0, nLen, "utf-8");
//				System.out.println(temp);
//			}
//			outstream.close();
//			instream.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			session.disconnect();
//			channel.disconnect();
//		}
//	}
//
//	public static void main(String[] args) {
//		try {
//			Runmanager runmanager = new Runmanager();
//			runmanager.setCommand("/root/nutch/nutch1.7/test.sh");
//			runmanager.setUsername("root");
//			runmanager.setPassword("richinfo@admin");
//			runmanager.setHostname("192.168.1.100");
//			runmanager.setPort(22);
//
//			// execCmd(runmanager);
//			// execCmd("hadoop fs -test -e data", "biadmin", "biadmin",
//			// "192.168.100.5");
//			// sshShell("192.168.1.74", "se", "richinfo+se",22 ,"" ,"");
//		} catch (Exception e) {
//			LOG.error(e.getMessage(), e);
//		}
//	}

}
