package com.intlime.mark.tools.push;

/**
 * 
 * @author wtuadn
 * @version v1.0
 * @date 2015/3/24
 */
public class PushMessage
{
	String data; // pushData

	public String getData()
	{
		return data;
	}

	public void setData(String data)
	{
		this.data = data;
	}

	public PushMessage(String data)
	{
		this.data = data;
	}

}
