package pers.xia.jregexp.engine;

import java.util.HashSet;
import java.util.Iterator;
/*
 * 默认结点的形状，包括\s \S \w \W \d \D
 */

public final class DefaultNode
{
	public static Node sNode = null;
	public static Node bNode = null;
	public static Node dNode = null;
	public static Node wNode = null;
	
	public static Node SNode = null;
	public static Node BNode = null;
	public static Node DNode = null;
	public static Node WNode = null;

    private static Node getRangeTree(String s, boolean reverse)
    {
        HashSet<Character> oneChars = new HashSet<Character>();
        HashSet<char[]> multiChars = new HashSet<char[]>();

        Node node = null;
        Node optNode = null;
        Node preNode = null;
        for(int i = 0; i < s.length(); i++)
        {
            char n = s.charAt(i);
            if(n == '-' || i >= s.length() - 1)
            {
                oneChars.add(n);
                continue;
            }
            char n1 = s.charAt(i + 1);

            //对于[1--9]这种情况当做[1-9]来处理
            if(n1 == '-')
            {
                i += 1;
                while(n1 == '-' && i < s.length()) n1 = s.charAt(++i);
                // 如果已经搜索到结束都是“-”说明是[--a]这种格式
                // 需要将a和-都存入单个字符中。
                if(n1 == '-')
                {
                    oneChars.add(n);
                    oneChars.add(n1);
                    break;
                }
                multiChars.add(new char[]{n, n1});
                continue;
            }
            oneChars.add(n);
        }

        if(!oneChars.isEmpty())
        {
            //获取set中的一个数据用于初始化node。
            Iterator<Character> iter = oneChars.iterator();
            char c1 = iter.next();
            node = new Node(c1);
            oneChars.remove(c1);
            for(char oneChar: oneChars)
            {
                preNode = new Node(oneChar);
                preNode.setReverse(reverse);
                optNode = new Node(Operator.OR);
                optNode.setLChild(node);
                optNode.setRChild(preNode);
                node = optNode;
            }
        }

        if(node == null && !multiChars.isEmpty())
        {
            Iterator<char[]> iter = multiChars.iterator();
            char []c1 = iter.next();
            node = new Node(c1[0], c1[1]);
            multiChars.remove(c1);
        }
        for(char[] multiChar: multiChars)
        {
            preNode = new Node(multiChar[0], multiChar[1]);
            preNode.setReverse(reverse);
            optNode = new Node(Operator.OR);
            optNode.setLChild(node);
            optNode.setRChild(preNode);
            node = optNode;
        }

        return node;
    }
	
	static
	{
        String s = " \t\r\n"; //\s中包含的字符
        String d = "0-9"; //\d中包含的字符
        String w = "a-zA-Z0-9_"; //\w中包含的字符

        sNode = getRangeTree(s, false);
        SNode = getRangeTree(s, true);
        dNode = getRangeTree(d, false);
        DNode = getRangeTree(d, true);
        wNode = getRangeTree(w, false);
        WNode = getRangeTree(w, true);

	}

}
