package javabot.javadoc;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

public class ListMap
{
	private Map map=new HashMap();

	public void put(Object key,Object value)
	{
		List l=(List)map.get(key);
		
		if (l==null)
			l = new LinkedList();
		
		l.add(value);
		map.put(key,l);
	}

	public List get(Object key)
	{
		return (List)map.get(key);
	}
}