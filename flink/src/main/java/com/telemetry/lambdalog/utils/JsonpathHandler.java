package com.telemetry.lambdalog.utils;

import com.jayway.jsonpath.JsonPath;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonpathHandler
{

	public static List<Map<String, Object>> getList(Object document, String path)
	{
		try
		{
			return JsonPath.read(document, path);
		}
		catch (Exception e)
		{
			return Collections.emptyList();
		}
	}

	public static String getValue(Object document, String path)
	{
		try
		{
			return JsonPath.read(document, path).toString();
		}
		catch (Exception e)
		{
			return "";
		}
	}

	public static String getValueIfString(Object document, String path)
	{
		try
		{
			return JsonPath.read(document, path);
		}
		catch (Exception e)
		{
			return "";
		}
	}

	public static List<String> getStringList(Object document, String path)
	{
		try
		{
			return JsonPath.read(document, path);
		}
		catch (Exception e)
		{
			return Collections.emptyList();
		}
	}

	public static Map<String, Object> getObject(Object document, String path)
	{
		try
		{
			return JsonPath.read(document, path);
		}
		catch (Exception e)
		{
			return Collections.emptyMap();
		}
	}
}
