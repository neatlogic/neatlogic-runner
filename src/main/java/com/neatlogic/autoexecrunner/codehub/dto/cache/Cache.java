/**   
 * @author      zouye
 * @date        2021-01-14   
 */
package com.neatlogic.autoexecrunner.codehub.dto.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.neatlogic.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import org.apache.commons.io.FileUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;


/**   
 * @ClassName   Cache   
 * @Description commit 与 diff 的缓存实现类
 * @author      zouye
 * @date        2021-01-14   
 *    
 */
public class Cache {
	private static Logger logger = LoggerFactory.getLogger(Cache.class);
	
	/** 文件名称中的非法字符，将会被替换为下划线 */
	private static String INVALID_CHARS = "\\/:*?\"<>|.";
	
	private String repositoryPathStr = null;
	
	private String cachePathStr = null;
	
	private String commitPathStr = null;
	
	private String diffPathStr = null;
	
	private File commitPath = null;
	
	private File diffPath = null;
	
	private File commitIndexFile = null;
	
	private File diffIndexFile = null;
	
	private File headFile = null;
	
	public Cache(Long repositoryServiceId, Long repositoryId, String branchName) {
		this.repositoryPathStr = String.format("%s/%s/.cache/%s/", Config.WORKING_COPY_PATH, repositoryServiceId, repositoryId);
		
		this.cachePathStr = repositoryPathStr + 
			StringUtils.replaceChars(branchName, INVALID_CHARS, StringUtils.repeat("_", INVALID_CHARS.length())) + "/";
		
		this.commitPathStr = this.cachePathStr + "commit/";
		this.diffPathStr = this.cachePathStr + "diff/";
	}
	
	public String getHead() {
		File file = new File(cachePathStr + "HEAD");
		if (file.exists()) {
			try {
				return FileUtils.readFileToString(file, String.valueOf(StandardCharsets.UTF_8));
			} catch (Exception ex) {
				logger.error(String.format("Read head commit from '%sHEAD' failed", cachePathStr), ex);
			}
		}
		
		return null;
	}
	
	public void setHead(String headCommitId) {
		try {
			File file = createHeadFile();
			FileUtils.writeStringToFile(file, headCommitId, String.valueOf(StandardCharsets.UTF_8), false);
		} catch (Exception ex) {
			logger.error("Save head failed", ex);
		}
	}
	
	private File createHeadFile() throws IOException {
		if (this.headFile == null) {
			if (createPath(cachePathStr) != null) {
				this.headFile = createFile(this.cachePathStr + "HEAD");
			}
		}
		
		return this.headFile;
	}
	
	private File createDiffPath() throws IOException {
		if (this.diffPath == null) {
			this.diffPath = createPath(diffPathStr);
		}
		
		return this.diffPath;
	}
	
	private File createCommitPath() throws IOException {
		if (this.commitPath == null) {
			this.commitPath = createPath(commitPathStr);
		}
		
		return this.commitPath;
	}
	
	private File createCommitIndexFile() throws IOException {
		if (this.commitIndexFile == null) {
			if (createCommitPath() != null) {
				this.commitIndexFile = createFile(commitPathStr + "index");
			}
		}
		
		return this.commitIndexFile;
	}
	
	private File createDiffIndexFile() throws IOException {
		if (this.diffIndexFile == null) {
			if (createDiffPath() != null) {
				this.diffIndexFile = createFile(diffPathStr + "index");
			}
		}
		
		return this.diffIndexFile;
	}
	
	private File createPath(String pathStr) throws IOException {
		File path = new File(pathStr);
		if (!path.exists() && !path.mkdirs()) {
			throw new IOException(String.format("create cache path '%s' failed.", pathStr));
		}
		
		return path;
	}
	
	private File createFile(String fileStr) throws IOException {
		File file = new File(fileStr);
		if (!file.exists() && !file.createNewFile()) {
			throw new IOException(String.format("create cache file '%s' failed.", fileStr));
		}
		
		return file;
	}
	
	public void writeGitDiffToCache(String leftCommitId, String rightCommitId, JSONArray content) {
		if (CollectionUtils.isEmpty(content) || StringUtils.isBlank(leftCommitId) || StringUtils.isBlank(rightCommitId)) {
			return;
		}
		
		try {
			File indexFile = new File(repositoryPathStr + "diff_index");
			if (!indexFile.exists()) {
				File path = new File(repositoryPathStr);
				if (!path.exists() && !path.mkdirs()) {
					throw new IOException(String.format("create cache path '%s' failed", path.getCanonicalFile()));
				}
				
				if (!indexFile.createNewFile()) {
					throw new IOException(String.format("create cache file '%s' failed", indexFile.getCanonicalFile()));
				}
			}
			
			File path = new File(repositoryPathStr + "diff");
			if (!path.exists()) {
				if (!path.exists() && !path.mkdirs()) {
					throw new IOException(String.format("create cache path '%s' failed", path.getCanonicalFile()));
				}
			}

			if (isDiffExists(indexFile, leftCommitId, rightCommitId)) {
				// 已经存在, 不进行写入操作
				return;
			}
			
			String[] fileNames = path.list();
			File file = null;
			int fileName = 1;
			if (fileNames != null && fileNames.length > 0) {
				for (String name: fileNames) {
					int n = Integer.parseInt(name);
					if (n > fileName) {
						fileName = n;
					}
				}
				
				file = new File(repositoryPathStr + "diff/" + fileName);
				if (file.length() > Config.CACHE_MAX_SIZE) {
					fileName = fileName + 1;
				}
			}
			
			file = new File(repositoryPathStr + "diff/" + fileName);
			if (!file.exists() && !file.createNewFile()) {
				throw new IOException(String.format("create cache file '%s' failed", file.getCanonicalFile()));
			}
			
			RandomAccessFile raf = null;
			FileChannel channel = null;
			try {
				raf = new RandomAccessFile(file, "rw");
			    channel = raf.getChannel();
			    
			    StringBuffer idxContent = new StringBuffer();
		    	ByteBuffer bytes = ByteBuffer.wrap(content.toString().getBytes(StandardCharsets.UTF_8));
				ByteBuffer length = ByteBuffer.allocate(4).putInt(bytes.array().length);
		
				long pos = raf.length();
				channel.position(pos);
				length.flip();
				channel.write(length);
				channel.write(bytes);
				
				idxContent.append(String.format("%s %s %s %s\n", leftCommitId.substring(0, 7), rightCommitId.substring(0, 7), fileName, pos));
			    
			    FileUtils.writeStringToFile(indexFile, idxContent.toString(), String.valueOf(StandardCharsets.UTF_8), true);
			    
			} catch (Exception ex) {
				throw new IOException(ex);
			} finally {
				try {
					if (channel != null) {
						channel.close();
					}
					
					if (raf != null) {
						raf.close();
					}
				} catch(Exception ex) {
					
				}
			}
		} catch (Exception ex) {
			logger.error("write commit cache to file failed", ex);
		}
	}
	
	public JSONArray readGitDiffFromCache(String leftCommitId, String rightCommitId) {
		if (StringUtils.isBlank(leftCommitId) || StringUtils.isBlank(rightCommitId)) {
			return null;
		}
		
		File indexFile = new File(repositoryPathStr + "diff_index");
		if (!indexFile.exists()) {
			return null;
		}
		
		String fileName = null;
		long pointer = 0;
		JSONArray retList = new JSONArray();
		
		InputStream in = null;
		InputStreamReader reader = null;
		BufferedReader r = null;
		try {
			in = new FileInputStream(indexFile);
			reader = new InputStreamReader(in);
			r = new BufferedReader(reader);
			
			String line = null;
			leftCommitId = leftCommitId.substring(0, 7);
			rightCommitId = rightCommitId.substring(0, 7);
			while ((line = r.readLine()) != null) {
				String[] locations = line.split(" ");
				if (locations[0].equals(leftCommitId) && locations[1].equals(rightCommitId)) {
					fileName = locations[2];
					pointer = Long.parseLong(locations[3]);
					
					break;
				}
			}
		} catch (Exception ex) {
			logger.error("Read cache from file failed", ex);
		} finally {
			try {
				if (r != null) {
					r.close();
				}
				if (reader != null) {
					reader.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (Exception ex) {
				
			}
		}
		
		if (StringUtils.isBlank(fileName)) {
			return null;
		}
		
		RandomAccessFile raf = null;
		FileChannel channel = null;
		File file = new File(repositoryPathStr + "diff/" + fileName);
		try {
			raf = new RandomAccessFile(file, "r");
		    channel = raf.getChannel();
	    	FileChannel fc = channel.position(pointer);

		    //get length of entry
		    ByteBuffer buffer = ByteBuffer.wrap(new byte[4]);
		    fc.read(buffer);
		    buffer.flip();
		    int length = buffer.getInt();

		    //read entry
		    buffer = ByteBuffer.wrap(new byte[length]);
		    fc.read(buffer);
		    buffer.flip();
		    
		    retList = JSONArray.parseArray(new String(buffer.array(), StandardCharsets.UTF_8));
		} catch (Exception ex) {
			logger.error("Read cache from file failed", ex);
		} finally {
			try {
				if (channel != null) {
					channel.close();
				}
				
				if (raf != null) {
					raf.close();
				}
			} catch(Exception ex) {
				
			}
		}
		
		return retList;
	}
	
	public Map<String, Object> readGitCommitsFromCache(List<String> commitList) {
		Map<String, Object> ret = new HashMap<>();
		ret.put("matchedCommitList", null);
		ret.put("mismatchCommitIdList", commitList);
		
		if (CollectionUtils.isEmpty(commitList)) {
			return ret;
		}
		
		File indexFile = new File(repositoryPathStr + "commit_index");
		if (!indexFile.exists()) {
			return ret;
		}
		
		Map<String, List<Long>> filePosMap = new HashMap<>();
		JSONArray retList = new JSONArray();
		
		InputStream in = null;
		InputStreamReader reader = null;
		BufferedReader r = null;
		try {
			in = new FileInputStream(indexFile);
			reader = new InputStreamReader(in);
			r = new BufferedReader(reader);
			
			String line = null;
			List<String> noMatchCommitList = new LinkedList<>(commitList);
			List<String> shortCommitList = new LinkedList<>();
			for (String commit: commitList) {
				shortCommitList.add(commit.substring(0, 7));
			}
			
			while (noMatchCommitList.size() > 0 && (line = r.readLine()) != null) {
				String[] locations = line.split(" ");
				
				for (int i = 0; i < shortCommitList.size(); i++) {
					if (locations[0].equals(shortCommitList.get(i))) {
						if (!filePosMap.containsKey(locations[1])) {
							filePosMap.put(locations[1], new ArrayList<>());
						}
						
						filePosMap.get(locations[1]).add(Long.parseLong(locations[2]));
						
						shortCommitList.remove(i);
						noMatchCommitList.remove(i);
						break;
					}
				}
			}
			
			ret.put("mismatchCommitIdList", noMatchCommitList);
		} catch (Exception ex) {
			logger.error("Read cache from file failed", ex);
		} finally {
			try {
				if (r != null) {
					r.close();
				}
				if (reader != null) {
					reader.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (Exception ex) {
				
			}
		}

		if (!filePosMap.isEmpty()) {
			for (String fileName: filePosMap.keySet()) {
				RandomAccessFile raf = null;
				FileChannel channel = null;
				File file = new File(repositoryPathStr + "commit/" + fileName);
				try {
					raf = new RandomAccessFile(file, "r");
				    channel = raf.getChannel();
				    for (Long pointer: filePosMap.get(fileName)) {
				    	FileChannel fc = channel.position(pointer);

					    //get length of entry
					    ByteBuffer buffer = ByteBuffer.wrap(new byte[4]);
					    fc.read(buffer);
					    buffer.flip();
					    int length = buffer.getInt();

					    //read entry
					    buffer = ByteBuffer.wrap(new byte[length]);
					    fc.read(buffer);
					    buffer.flip();
					    
					    JSONObject obj = JSONObject.parseObject(new String(buffer.array(), StandardCharsets.UTF_8));
					    retList.add(obj);
				    }
				} catch (Exception ex) {
					logger.error("Read cache from file failed", ex);
				} finally {
					try {
						if (channel != null) {
							channel.close();
						}
						
						if (raf != null) {
							raf.close();
						}
					} catch(Exception ex) {
						
					}
				}
			}
		}
		
		ret.put("matchedCommitList", retList);
		return ret;
	}
	
	public void writeGitCommitsToCache(List<CommitInfo> content) {
		if (CollectionUtils.isEmpty(content)) {
			return;
		}
		
		try {
			File indexFile = new File(repositoryPathStr + "commit_index");
			if (!indexFile.exists()) {
				File path = new File(repositoryPathStr);
				if (!path.exists() && !path.mkdirs()) {
					throw new IOException(String.format("create cache path '%s' failed", path.getCanonicalFile()));
				}
				
				if (!indexFile.createNewFile()) {
					throw new IOException(String.format("create cache file '%s' failed", indexFile.getCanonicalFile()));
				}
			}
			
			File path = new File(repositoryPathStr + "commit");
			if (!path.exists()) {
				if (!path.exists() && !path.mkdirs()) {
					throw new IOException(String.format("create cache path '%s' failed", path.getCanonicalFile()));
				}
			}
			
			String[] fileNames = path.list();
			File file = null;
			int fileName = 1;
			if (fileNames != null && fileNames.length > 0) {
				for (String name: fileNames) {
					int n = Integer.parseInt(name);
					if (n > fileName) {
						fileName = n;
					}
				}
				
				file = new File(repositoryPathStr + "commit/" + fileName);
				if (file.length() > Config.CACHE_MAX_SIZE) {
					fileName = fileName + 1;
				}
			}
			
			file = new File(repositoryPathStr + "commit/" + fileName);
			if (!file.exists() && !file.createNewFile()) {
				throw new IOException(String.format("create cache file '%s' failed", file.getCanonicalFile()));
			}
			
			RandomAccessFile raf = null;
			FileChannel channel = null;
			try {
				raf = new RandomAccessFile(file, "rw");
			    channel = raf.getChannel();
			    
			    StringBuffer idxContent = new StringBuffer();
			    for (int i = 0; i < content.size(); i++) {
			    	CommitInfo commit = content.get(i);
			    	String commitId = commit.getCommitId().substring(0, 7);
			    	ByteBuffer bytes = ByteBuffer.wrap(JSON.toJSONString(commit, SerializerFeature.DisableCircularReferenceDetect).getBytes(StandardCharsets.UTF_8));
					ByteBuffer length = ByteBuffer.allocate(4).putInt(bytes.array().length);
			
					long pos = raf.length();
					channel.position(pos);
					length.flip();
					channel.write(length);
					channel.write(bytes);
					
					idxContent.append(String.format("%s %s %s\n", commitId, fileName, pos));
			    }
			    
			    FileUtils.writeStringToFile(indexFile, idxContent.toString(), String.valueOf(StandardCharsets.UTF_8), true);
			    
			} catch (Exception ex) {
				throw new IOException(ex);
			} finally {
				try {
					if (channel != null) {
						channel.close();
					}
					
					if (raf != null) {
						raf.close();
					}
				} catch(Exception ex) {
					
				}
			}
		} catch (Exception ex) {
			logger.error("write commit cache to file failed", ex);
		}
	}
	
	public void writeCommitsToCache(JSONArray content) {
		writeCommitsToCache(null, content);
	}
	
	/**
	 * 
	 *
	 * @param content
	 * @throws IOException      
	 */
	@SuppressWarnings("unchecked")
	public void writeCommitsToCache(String srcStartCommitId, JSONArray content) {
		
		try {
			File path = createCommitPath();
			content.sort((o1, o2) -> {
				JSONObject obj1 = (JSONObject) JSON.toJSON(o1);
				JSONObject obj2 = (JSONObject) JSON.toJSON(o2);
				return obj2.getLong("commitId").compareTo(obj1.getLong("commitId"));
			});

			if (StringUtils.isBlank(srcStartCommitId)) {
				srcStartCommitId = content.getJSONObject(0).getString("commitId");
			}
			
			String[] fileNames = path.list();
			if (fileNames == null || fileNames.length == 0) { // 缓存文件不存在
				File file = new File(commitPathStr + srcStartCommitId);
				try {
					if (!file.createNewFile()) {
						throw new IOException(String.format("create cache file '%s' failed.", file.getAbsolutePath()));
					}
					
					FileUtils.writeStringToFile(file, content.toString(), String.valueOf(StandardCharsets.UTF_8));
				} catch (IOException e) {
					throw new IOException(String.format("write commit cache to '%s' failed, %s", file.getAbsolutePath(), e.getMessage()), e);
				}
			} else {
				List<Long> commitIdList = new ArrayList<>();
				for (String commitId: fileNames) {
					commitIdList.add(Long.parseLong(commitId));
				}
				
				Collections.sort(commitIdList);
				
				long srcCommitIdNum = Long.parseLong(srcStartCommitId);
				int idx = commitIdList.size() - 1;
				// 缓存数量大于1
				if (commitIdList.size() > 1) {
					for (int i = commitIdList.size() - 2; i >= 0; i--) {
						// 传入的 commitId 比次新还新，那就取最新的缓存
						if (srcCommitIdNum > commitIdList.get(i)) {
							idx = i + 1;
							break;
						}
					}
				}
				
				String fileName = String.valueOf(commitIdList.get(idx));
				File oldFile = new File(commitPathStr + fileName);
				File newFile = new File(commitPathStr + srcStartCommitId);
				
				// 文件已超过大小，创建新文件
				if (oldFile.length() >= Config.CACHE_MAX_SIZE) {
					try {
						if (!newFile.createNewFile()) {
							System.err.println(String.format("create commit cache '%s' failed.", newFile.getAbsolutePath()));
							return;
						}
						
						FileUtils.writeStringToFile(newFile, content.toString(), String.valueOf(StandardCharsets.UTF_8));
					} catch (IOException e) {
						throw new IOException(String.format("write commit cache to '%s' failed, %s", newFile.getAbsolutePath(), e.getMessage()), e);
					}
				} else {
					String oldContent = FileUtils.readFileToString(oldFile, String.valueOf(StandardCharsets.UTF_8));
					
					if (StringUtils.isNotBlank(oldContent)) {
						JSONArray arr = JSONArray.parseArray(oldContent);
						content.addAll(arr);
					}
					
					FileUtils.writeStringToFile(oldFile, content.toString(), String.valueOf(StandardCharsets.UTF_8));
					oldFile.renameTo(newFile);
				}
			}
		} catch(Exception ex) {
			logger.error("Write commit to cache failed", ex);
		}
	}
	
	/**
	 * 更新模式保存 svn commit 缓存，如果缓存不存在插入 。
	 * @param content
	 * @throws Exception      
	 */
	@SuppressWarnings("unchecked")
	public void updateSVNCommitsToCache(String srcStartCommitId, JSONArray content) {
		try {
			JSONArray saveContent = new JSONArray();
			long srcCommitIdNum = Long.parseLong(srcStartCommitId);
			boolean needChangeName = false;
			
			String fileName = null;
			
			File path = new File(commitPathStr);
			String[] fileNames = path.list();
			if (fileNames == null || fileNames.length == 0) { // 缓存文件不存在
				fileName = srcStartCommitId;
			} else {
				List<Long> commitIdList = new ArrayList<>();
				for (String commitId: fileNames) {
					commitIdList.add(Long.parseLong(commitId));
				}
				
				Collections.sort(commitIdList);
				
				int idx = 0;
				// 找到要更新的缓存文件
				if (commitIdList.size() > 1) {
					for (int i = commitIdList.size() - 2; i >= 0; i--) {
						// 传入的 commitId 比次新还新，那就取最新的缓存
						if (srcCommitIdNum > commitIdList.get(i)) {
							idx = i + 1;
							break;
						}
					}
				}
				
				fileName = String.valueOf(commitIdList.get(idx));
				File oldFile = new File(commitPathStr + fileName);
				
				String oldContent = FileUtils.readFileToString(oldFile, String.valueOf(StandardCharsets.UTF_8));
				
				if (StringUtils.isNotBlank(oldContent)) {
					JSONArray arr = JSONArray.parseArray(oldContent);
					
					Iterator<?> oldIt = arr.iterator();
					Iterator<?> newIt = content.iterator();
					
					Map<String, Object> commitMap = new HashMap<>();
					
					while(newIt.hasNext()) {
						JSONObject n = (JSONObject)newIt.next();
						commitMap.put(n.getString("commitId"), n);
					}
					
					while(oldIt.hasNext()) {
						JSONObject o = (JSONObject)oldIt.next();
						String commitId = o.getString("commitId");
						if (!commitMap.containsKey(commitId)) {
							commitMap.put(commitId, o);
						}
					}
					
					for (String commitId: commitMap.keySet()) {
						saveContent.add(commitMap.get(commitId));
					}
					
					if (srcCommitIdNum > commitIdList.get(idx)) {
						needChangeName = true;
					}
				}
			}
			
			if (saveContent.size() > 0) {
				
				File file = new File(commitPathStr + fileName);
				if (file.exists()) {
					if (needChangeName) {
						File newFile = new File(commitPathStr + srcCommitIdNum);
						if (file.renameTo(newFile)) {
							// fix bug: 重命名成功了, 旧的file变量需要指向最新的文件名, 否则下面代码旧文件还会写入重新生成
							file = newFile;
						} else {
							throw new IOException(String.format("renameTo cache path '%s' failed", file.getCanonicalPath()));
						}

					}
				} else {
					if (!path.exists()) {
						if (!path.mkdirs()) {
							throw new IOException(String.format("create cache path '%s' failed", path.getCanonicalPath()));
						}
					}
					
					if(!file.createNewFile()) {
						throw new IOException(String.format("create cache file '%s' failed", file.getCanonicalPath()));
					}
				}

				// 根据 commit id 倒序存放，大的在前，小的在后
				saveContent.sort((o1, o2) -> {
					JSONObject obj1 = (JSONObject) JSON.toJSON(o1);
					JSONObject obj2 = (JSONObject) JSON.toJSON(o2);
					return obj2.getLong("commitId").compareTo(obj1.getLong("commitId"));
				});
				
				FileUtils.writeStringToFile(file, saveContent.toString(), String.valueOf(StandardCharsets.UTF_8), false);
			}
		} catch (Exception ex) {
			logger.error("Update commit to cache failed", ex);
		}
	}
	
	/** 从缓存读取 commits，保存时应保证新的 commit 排在前面 */
	public JSONArray getSVNCommitsFromCache(String srcStartCommitId, String srcEndCommitId, int size) {
		try {
			File path = new File(commitPathStr);
			
			if (!path.exists()) {
				return null;
			}
			
			String[] fileNames = path.list();
			if (fileNames == null || fileNames.length == 0) {
				return null;
			}
			
			Long srcStartCommitIdNum = Long.parseLong(srcStartCommitId);
			Long srcEndCommitIdNum = Long.parseLong(srcEndCommitId);
			List<Long> commitIdList = new ArrayList<>();
			for (String commitId: fileNames) {
				commitIdList.add(Long.parseLong(commitId));
			}
			
			Collections.sort(commitIdList);
			
			JSONArray commits = new JSONArray();
			int idx = commitIdList.size() - 1;
			
			// endCommit 都比已经缓存的最大的 commit 还大，说明没有想要的内容
			if (srcEndCommitIdNum > commitIdList.get(idx)) {
				return null;
			}
			
			// 缓存数量大于1
			if (commitIdList.size() > 1) {
				for (int i = commitIdList.size() - 2; i >= 0; i--) {
					// 传入的 commitId 比次新还新，那就取最新的缓存
					if (srcStartCommitIdNum > commitIdList.get(i)) {
						idx = i + 1;
						break;
					}
				}
			}
			
			// 缓存文件中新的 commit 在文件前面，老的在后面
			while(idx >= 0 && srcEndCommitIdNum <= commitIdList.get(idx) && commits.size() < size) {
				String content = FileUtils.readFileToString(new File(commitPathStr + commitIdList.get(idx)), String.valueOf(StandardCharsets.UTF_8));
				if (StringUtils.isNotBlank(content)) {
					JSONArray arr = JSONArray.parseArray(content);
					if (commits.size() + arr.size() <= size) {// e.g. 100 20 115
						for (int i = 0; i < arr.size(); i++) {

							// commit列表id是从大到小排列
							// 如果过大那么需要继续找, 跳过这个
							if (arr.getJSONObject(i).getLong("commitId") > srcStartCommitIdNum) {
								continue;
							}
							// 如果已经取到最够小的commit就需要停止了
							if (arr.getJSONObject(i).getLong("commitId") < srcEndCommitIdNum) {
								break;
							}
							commits.add(arr.get(i));
						}
					} else {
						// fix：缓存里面有30个commit，页面设置搜索commit数量为5，实际只从缓存中取了3个commit
						int len = size - commits.size();
						for (int i = 0; i < len; i++) {
							if (arr.getJSONObject(i).getLong("commitId") > srcStartCommitIdNum) {
								continue;
							}
							if (arr.getJSONObject(i).getLong("commitId") < srcEndCommitIdNum) {
								break;
							}
							commits.add(arr.get(i));
						}
					}
				}
				
				idx--;
			}
			
			return commits;
		} catch (Exception ex) {
			logger.error("Read commit from cache failed", ex);
		}
		
		return null;
	}

	/** diff 只会 写入，不存在update 的情况 */
	public void writeSVNDiffToCache(String leftCommitId, String rightCommitId, JSONArray content) {
		try {
			File path = new File(diffPathStr);
			
			if (!path.exists() && !path.mkdirs()) {
				throw new IOException(String.format("create cache path '%s' failed", diffPathStr));
			}
			
			File indexFile = createDiffIndexFile();

			if (isDiffExists(indexFile, leftCommitId, rightCommitId)) {
				// 已经存在, 不进行写入操作
				return;
			}
			
			// 索引文件的作用是定位某个 diff 在哪个文件的哪个位置
			// 格式：leftCommitId..rightCommitId <diff文件名称> <在文件中的位置>
			// 100981..100980 1 0
			// 100982.。100981 1 2000
			String diffRange = String.format("%s..%s", leftCommitId, rightCommitId);
			String[] fileNames = path.list();
			int latestFileNum = 1;
			
			// 找到最新的缓存 diff 文件
			for (String fileName: fileNames) {
				if (fileName.equals("index")) {
					continue;
				}
				
				int curr = Integer.parseInt(fileName);
				if (latestFileNum < curr) {
					latestFileNum = curr;
				}
			}
			
			File lastestCacheFile = new File(diffPathStr + latestFileNum);
			if (!lastestCacheFile.exists() && !lastestCacheFile.createNewFile()) {
				throw new IOException(String.format("create cache file '%s' failed.", lastestCacheFile.getAbsolutePath()));
			}
			
			if (lastestCacheFile.length() >= Config.CACHE_MAX_SIZE) { // 超过最大缓存限制，创建新文件
				latestFileNum = latestFileNum + 1;
				lastestCacheFile = new File(diffPathStr + latestFileNum);
				if (!lastestCacheFile.createNewFile()) {
					throw new IOException(String.format("create cache file '%s' failed.", lastestCacheFile.getAbsolutePath()));
				}
			}
			
			long pos = writeToFile(lastestCacheFile, content.toString());
			
			// 将缓存位置信息追加到索引文件中
			FileUtils.writeStringToFile(indexFile, String.format("%s %s %s\n", diffRange, latestFileNum, pos), String.valueOf(StandardCharsets.UTF_8), true);
		} catch(Exception ex) {
			logger.error("Write diff to cache failed", ex);
		}
	}
	
	public JSONArray getSVNDiffFromCache(String leftCommitId, String rightCommitId) {
		try {
			File path = new File(diffPathStr);
			
			if (!path.exists()) {
				return null;
			}
			
			File indexFile = new File(diffPathStr + "index");
			if (!indexFile.exists()) {
				return null;
			}
			
			String diffRange = String.format("%s..%s", leftCommitId, rightCommitId);
			String diffFileName = null;
			int pointer = 0;
			
			String line = null;
			
			InputStreamReader isr = null;
			BufferedReader br = null;
			InputStream in = null;
			try {
				in = new FileInputStream(indexFile);
				isr = new InputStreamReader(in);
				br = new BufferedReader(isr);
				while((line = br.readLine()) != null) {
					String[] location = line.split(" ");
					if (StringUtils.equals(diffRange, location[0])) {
						diffFileName = location[1];
						pointer = Integer.parseInt(location[2]);
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null) {
						br.close();
					}
					
					if (isr != null) {
						isr.close();
					}
					
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
				}
			}
			
			if (StringUtils.isBlank(diffFileName)) {
				return null;
			}
			
			String content = readFromFile(new File(diffPathStr + diffFileName), pointer);
			if (StringUtils.isNotBlank(content)) {
				return JSONArray.parseArray(content);
			}
		} catch(Exception ex) {
			logger.error("Read diff from catch failed", ex);
		}

		return null;
	}
	
	/** 从文件的指定位置读取特定长度的内容 */
	private String readFromFile(File file, long pointer) throws IOException {
		RandomAccessFile raf = null;
		FileChannel channel = null;
		try {
			raf = new RandomAccessFile(file, "r");
		    channel = raf.getChannel();
		    FileChannel fc = channel.position(pointer);

		    //get length of entry
		    ByteBuffer buffer = ByteBuffer.wrap(new byte[4]);
		    fc.read(buffer);
		    buffer.flip();
		    int length = buffer.getInt();

		    //read entry
		    buffer = ByteBuffer.wrap(new byte[length]);
		    fc.read(buffer);
		    buffer.flip();
			
		    return new String(buffer.array(), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			throw new IOException(ex);
		} finally {
			try {
				if (channel != null) {
					channel.close();
				}
				
				if (raf != null) {
					raf.close();
				}
			} catch(Exception ex) {
				
			}
		}
	}
	
	/** 将字符串内容及其长度信息存入文件，返回内容在文件中的位置 */
	private long writeToFile(File file, String str) throws IOException {
		RandomAccessFile raf = null;
		FileChannel channel = null;
		try {
			raf = new RandomAccessFile(file, "rw");
		    channel = raf.getChannel();
			ByteBuffer bytes = ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8));
		    // 字符串的长度
			ByteBuffer length = ByteBuffer.allocate(4).putInt(bytes.array().length);
	
			long pos = raf.length();
			channel.position(pos);
			length.flip();
			channel.write(length);
			channel.write(bytes);
			
			return pos;
		} catch (Exception ex) {
			throw new IOException(ex);
		} finally {
			try {
				if (channel != null) {
					channel.close();
				}
				
				if (raf != null) {
					raf.close();
				}
			} catch(Exception ex) {
				
			}
		}
	}


	/**
	 * 
	 * 判断diff内容是否已经存在index文件中, 如果已经存在, 那么不写入直接返回
	 * @param indexFile 
	 * @param leftCommitId
	 * @param rightCommitId
	 * @return
	 */
	public boolean isDiffExists(File indexFile, String leftCommitId, String rightCommitId) throws IOException {
		InputStream in = null;
		InputStreamReader reader = null;
		BufferedReader r = null;
		boolean result = false;
		try {
			in = new FileInputStream(indexFile);
			reader = new InputStreamReader(in);
			r = new BufferedReader(reader);
			String line = null;
			while ((line = r.readLine()) != null) {
				String[] locations = line.split(" ");
				if (locations.length == 4) {
					// Git
					String left = leftCommitId.substring(0, 7);
					String right = rightCommitId.substring(0, 7);
					if (locations[0].equals(left) && locations[1].equals(right)) {
						result = true;
						break;
					}
				} else if (locations.length == 3) {
					// SVN
					String diffRange = String.format("%s..%s", leftCommitId, rightCommitId);
					if (locations[0].equals(diffRange)) {
						result = true;
						break;
					}
				} else {
					throw new ApiRuntimeException("Index file format error: " + line);
				}
			}
		} finally {
			try {
				if (r != null) {
					r.close();
				}
				if (reader != null) {
					reader.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (Exception ignored) {

			}
		}
		return result;
	}
}
