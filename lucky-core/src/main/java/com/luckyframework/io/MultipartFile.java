package com.luckyframework.io;

import com.luckyframework.common.StringUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MultipartFile implements InputStreamSource {

	/** 用户上传的文件对应的输入流*/
	private final InputStream originalFileInputStream;
	/** 文件上传到服务器后的文件名*/
	private String uploadFileName;
	/** 文件类型*/
	private final String fileType;
	/** 原始的文件名*/
	private final String originalFileName;

	/**
	 *
	 * @param originalFileInputStream
	 * @param filename
	 */
	public MultipartFile(InputStream originalFileInputStream, String filename) {
		this.originalFileInputStream=originalFileInputStream;
		this.originalFileName=filename;
		int index = filename.lastIndexOf(".");
		this.fileType=filename.substring(index);
		this.uploadFileName = filename.substring(0,index)+"-"+ UUID.randomUUID().toString().replaceAll("-","")+fileType;
	}

	/**
	 * 获得上传文件的类型
	 * @return
	 */
	public String getFileType() {
		return this.fileType;
	}
	
	/**
	 * 获得上传到服务器后的文件名
	 * @return
	 */
	public String getFileName() {
		return uploadFileName;
	}

	/**
	 *获得文件的原始文件名
	 * @return
	 */
	public String getOriginalFileName() {
		return originalFileName;
	}

	/**
	 * 设置上传后的文件名<br/>
	 * 该方法会检测传入的文件名是否符合当前的文件类型，如果符合则直接采用，否则会自动加上文件后缀
	 * @param fileName 上传后文件在服务器中的文件名
	 */
	public void setFileName(String fileName){
		String fileType = getFileType();
		uploadFileName=fileName.endsWith(fileType)?fileName:fileName+fileType;
	}

	/**
	 * 将文件复制到系统的任意位置上文件夹中
	 * @param filepath 绝对路径
	 * @throws IOException
	 */
	public void copyToFolder(String filepath) throws IOException {
		File file=new File(filepath);
		copyToFolder(file);
	}
	
	private void copyToFolder(File folder) throws IOException {
		if(folder.isFile()){
			throw new RuntimeException("文件上传错误！ Message : 错误的文件夹："+folder.getAbsolutePath());
		}
		if(!folder.exists())
			folder.mkdirs();
		FileOutputStream outfile=new FileOutputStream(folder.getAbsoluteFile()+File.separator+uploadFileName);//projectPath+"/"+docRelativePath+"/"+uploadFileName);
		FileCopyUtils.copy(originalFileInputStream,outfile);
	}

	/**
	 * 获得上传文件的大小
	 * @return
	 * @throws IOException 
	 */
	public int getFileSize() throws IOException {
		return originalFileInputStream.available();
	}
	
	/**
	 * 获得文件对应的InputStream
	 * @return
	 */
	public InputStream getInputStream() {
		return originalFileInputStream;
	}

	/** 获得文件对应的byte数组 */
	public byte[] getByte() throws IOException {
		return FileCopyUtils.copyToByteArray(originalFileInputStream);
	}

	@Override
	public String toString() {
		int fileSize=0;
		try {
			fileSize = getFileSize();
		} catch (IOException e) {
		}
		return StringUtils.format("[{0}k] {1}",fileSize,getOriginalFileName());
	}

}
