package pers.xia.jregexp.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Stack;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Comparator;

public class GrammerTree
{
    //为什么Java中enum类型不能定义在函数内部？？！！！明明内部类都能创建！！！
    //Location只在splitMulti中使用，FRONT表示一个字符范围的开头，BELOW表示结尾
    //BOTH表示同时在两边存在
    private enum Location{FRONT, BELOW, BOTH};

    private static final Logger log =
        Logger.getLogger(GrammerTree.class.getName());
//    Stack<Character> inputStack = new Stack<Character>();
    String reString;
	public Node head = null;
    public int[] charClass = null;
	Stack<Node> resultStack = new Stack<Node>();
    private int cur = 0;

    char getNextC()
    {
        if(cur < 0)   
        {
            return '\0';
        }
            return this.reString.charAt(cur--);
    }

    boolean backupC(char c)
    { 
        if (c == '\0')
        {
            return true;
        }
        if(this.cur >= this.reString.length())
        {
            log.warning("ERROR: cur point out of range");
            return false;
        }
        this.cur++;
        return true;
    }

	GrammerTree(String input)
	{
        log.setLevel(Level.ALL);
        
        this.reString = input;
        this.cur = this.reString.length() - 1;
		if (!this.createGrammerTree())
		{
            log.warning("ERROR");
        }
	}

    //将范围字符和单个字符进行分割，并返回由小到大排列分割后的范围字符
    private ArrayList<Integer[]> splitMulti(HashSet<Integer[]> multiChars, HashSet<Integer>singleChars)
    {
        if(multiChars.isEmpty())
        {
            ArrayList<Integer> sc = new ArrayList<Integer>(singleChars);
            ArrayList<Integer[]> mc2 = new ArrayList<Integer[]>(); //保存加上单个结点后的数据
            Collections.sort(sc);

            for(int s: sc)
            {
                mc2.add(new Integer[]{s, s});
            }
            return mc2;
        }
        ArrayList<Integer[]> mc = new ArrayList<Integer[]>(multiChars);
        ArrayList<Integer[]> mc1 = new ArrayList<Integer[]>();

        //numMap用于记录数字在分割块中的位置
        HashMap<Integer, Location> numMap = new HashMap<Integer, Location>();

        //将mc中的数据按照第一个值由小到大进行排序
        Collections.sort(mc, new Comparator<Integer []>()
		{

			@Override
			public int compare(Integer[] o1, Integer[] o2)
			{
				return o1[0] - o2[0];
			}
		});

        //找分割点，分割块中所有的数据拿出来然后从小到大分割
        int maxNum = mc.get(0)[1];
        for(int i = 0; i < mc.size(); i++)
        {
            if(mc.get(i)[0] <= maxNum)
            {
                maxNum = maxNum > mc.get(i)[1] ? maxNum : mc.get(i)[1];
                
                //将mc中的两端的值放入到numMap中
                if(numMap.containsKey(mc.get(i)[0]))
                {
                    if(numMap.get(mc.get(i)[0]) == Location.BELOW)
                    {
                        numMap.put(mc.get(i)[0], Location.BOTH);
                    }
                }
                else
                {
                    numMap.put(mc.get(i)[0], Location.FRONT);
                }

                if(numMap.containsKey(mc.get(i)[1]))
                {
                    if(numMap.get(mc.get(i)[1]) == Location.FRONT)
                    {
                        numMap.put(mc.get(i)[1], Location.BOTH);
                    }
                }
                else
                {
                    numMap.put(mc.get(i)[1], Location.BELOW);
                }

                continue;
            }

            //将set转换为list，并排序，方便接下来的运算。
            ArrayList<Integer> numsList = new ArrayList<Integer>(numMap.keySet());
            Collections.sort(numsList);

            //将这些数据从小到大对数据进行分割。
            for(int j = 1; j < numsList.size(); j++)
            {
                int front = numsList.get(j-1);
                int below = numsList.get(j);

                if(numMap.get(front) == Location.BELOW)
                {
                    //如果某个点是mutliChar的结束位置，那么该点需要放到
                    //前一个结点中，此处的点需要后移一位
                    front++;
                }
                else if(numMap.get(front) == Location.BOTH)
                {
                    //如果某个点分别是两个multiChar的结束位置和起始位置，
                    //那么需要把这个点单独取出来
                    mc1.add(new Integer[]{front, front});
                    front++;
                }

                if(numMap.get(below) == Location.FRONT ||
                        numMap.get(below) == Location.BOTH)
                {
                    //同上，BOTH的操作放到下一个结点进行
                    below--;
                }
                if(front > below)
                {
                    continue;
                }
                mc1.add(new Integer[]{front, below});
            }

            //设置最大值为下一个字符范围的较大值。
            maxNum = mc.get(i)[1];
            numMap.clear();
            i--;
        }

        //循环结束后对nums中剩余的数据再进行一次操作。
        ArrayList<Integer> numsList = new ArrayList<Integer>(numMap.keySet());
        Collections.sort(numsList);

        //将这些数据从小到大对数据进行分割。
        for(int j = 1; j < numsList.size(); j++)
        {
            int front = numsList.get(j-1);
            int below = numsList.get(j);

            if(numMap.get(front) == Location.BELOW)
            {
                //如果某个点是mutliChar的结束位置，那么该点需要放到
                //前一个结点中，此处的点需要后移一位
                front++;
            }
            else if(numMap.get(front) == Location.BOTH)
            {
                //如果某个点分别是两个multiChar的结束位置和起始位置，
                //那么需要把这个点单独取出来
                mc1.add(new Integer[]{front, front});
                front++;
            }

            if(numMap.get(below) == Location.FRONT ||
                    numMap.get(below) == Location.BOTH)
            {
                //同上，BOTH的操作放到下一个结点进行
                below--;
            }

            if(front > below)
            {
                continue;
            }
            mc1.add(new Integer[]{front, below});
        }

        //将single chars中的值也加入表中

        ArrayList<Integer> sc = new ArrayList<Integer>(singleChars);
        ArrayList<Integer[]> mc2 = new ArrayList<Integer[]>(); //保存加上单个结点后的数据
        Collections.sort(sc);

        int i = 0;
        int j = 0;
        for(; i < sc.size(); i++)
        {
            for(; j < mc1.size(); j++)
            {
                if(sc.get(i) < mc1.get(j)[0])
                {
                    mc2.add(new Integer[]{sc.get(i), sc.get(i)});
                    break;
                }

                // 这里包含了三种情况，sc.get(i)的值与mc1.get(j)[0]的值相
                // 等，比mc1.get(j)[0]大但是比mc1.get(j)[1]小，与mc1.get(j)[1]
                // 相等三种情况，其中第一种情况不存在add mc1.get(j)[0]~sc.get(j)-1
                // 的操作，第三种情况存在add mc1.get(j)[0] = sc.get(i)+1的情况
                if(sc.get(i) <= mc1.get(j)[1])
                {
                    if(sc.get(i) > mc1.get(j)[0]) 
                        mc2.add(new Integer[]{mc1.get(j)[0], sc.get(i)-1});

                    mc2.add(new Integer[]{sc.get(i), sc.get(i)});

                    if(sc.get(i) < mc1.get(j)[1])
                        mc1.get(j)[0] = sc.get(i)+1;

                    mc1.get(j)[0] = sc.get(i)+1;

                    if(mc1.get(j)[0] > mc1.get(j)[1])
                    {
                        i++;
                    }
                    break;
                }
                //如果sc.get(i)的值在原范围的右侧的话就直接把该范围放入到mc2中
                mc2.add(mc1.get(j));
            }
        }

        //将剩余的数据放入到m2中
        for(; i < sc.size(); i++)
        {
            mc2.add(new Integer[]{sc.get(i), sc.get(i)});
        }

        for(; j < mc1.size(); j++)
        {
            mc2.add(mc1.get(j));
        }

        //return new HashSet<Integer[]>(mc2);
        return mc2;
    }

    private boolean changeCharToInt()
    {
        Stack<Node> stack = new Stack<Node>();
        Node node = this.head;

        stack.push(node);
        while(!stack.empty())
        {
            node = stack.pop();
            if(node.nodeType() == NodeType.CHAR)
            {
                node.value = (int)((Character)node.value);
            }
            else if(node.nodeType() == NodeType.MULTICHARS)
            {
                node.value = (int)((Character)node.value);
                node.value2 = (int)((Character)node.value2);
            }
            else if(node.nodeType() == NodeType.OPERATOR)
            {
                if(node.getLChild() != null) stack.push(node.getLChild());
                if(node.getRChild() != null) stack.push(node.getRChild());
            }
        }
        return true;
    }


    //处理NOT结点，将NOT结点下面的结点取反
    private boolean dealNotNode()
    {
        Stack<Node> stack = new Stack<Node>();

        Node node = this.head;

        Node lNode = null;
        Node rNode = null;

        stack.push(node);
        while(!stack.empty())
        {
            node = stack.pop();
            if (node.nodeType() == NodeType.OPERATOR)
            {
                if(node.value != Operator.NOT)
                {
                    lNode = node.getLChild();
                    rNode = node.getRChild();

                    if(lNode != null) stack.push(lNode);
                    if(rNode != null) stack.push(rNode);
                    continue;
                }

                Node notNode = node;
                HashSet<Integer> singleChars = new HashSet<Integer>();  //保存单个的字符
                HashSet<Integer[]> multiChars = new HashSet<Integer[]>();   //保存范围字符
                Stack<Node> stack2 = new Stack<Node>();                     //保存在NOT结点下面的其他结点

                stack2.push(node.getLChild());
                while(!stack2.empty())
                {
                    node = stack2.pop();
                    if(node.nodeType() == NodeType.OPERATOR)
                    {
                        if(node.value != Operator.OR)
                        {
                            log.warning("Have operator without OR below NOT operator");
                            return false;
                        }
                        lNode = node.getLChild();
                        rNode = node.getRChild();
                        if(lNode != null) stack2.push(lNode);
                        if(rNode != null) stack2.push(rNode);
                        continue;
                    }
                    if(node.nodeType() == NodeType.CHAR)
                    {
                        singleChars.add((int)node.value);
                        continue;
                    }
                    if(node.nodeType() == NodeType.MULTICHARS)
                    {
                        //将类型强制转换为Integer类型，方便接下来的操作
                        multiChars.add(new Integer[]{(int)node.value, 
                        		(int)node.value2});
                        continue;
                    }

                    log.warning("Have invaild node type below NOT operator");
                    return false;
                }
                //
                //获取到所有not结点下面的数据后将其进行分割操作
                //并获取到范围数据
                ArrayList<Integer[]> resultList = this.splitMulti(multiChars, singleChars);

                //开始取反
                ArrayList<Integer[]> antiList = new ArrayList<Integer[]>(); //保存取反后的数据
                int minNum = 0;
                for(Integer[] a : resultList)
                {
                    if(a[0] - minNum > 0)
                    {
                        antiList.add(new Integer[]{minNum, a[0]-1});
                    }
                    minNum = a[1] + 1;
                }
                //把最后的数字加上去
                if(minNum <= 65535)
                {
                    //XXX java默认采用unicode编码，为两个字节，如果改为utf-8的话则会
                    //变成3个字节，这里假设了长度为2个字节
                    antiList.add(new Integer[]{minNum, 65535}); 
                }

                //拼接结点
                Node preNode= new Node(antiList.get(0)[0], antiList.get(0)[1]);
                preNode.setNodeType(NodeType.MULTICHARS);
                Node nextNode = null;
                Node optNode = null;
                for(int i = 1; i < antiList.size(); i++)
                {
                    nextNode = new Node(antiList.get(i)[0], antiList.get(i)[1]);
                    nextNode.setNodeType(NodeType.MULTICHARS);
                    optNode = new Node(Operator.OR);
                    optNode.setLChild(preNode);
                    optNode.setRChild(nextNode);
                    preNode = optNode;
                }

                if(notNode == this.head)
                {
                    this.head = preNode;
                }
                else
                {
                    Node fatherNode = notNode.fatherNode;
                    if(fatherNode.getLChild() == notNode)
                    {
                        fatherNode.setLChild(preNode);
                    }
                    else if(fatherNode.getRChild() == notNode)
                    {
                        fatherNode.setRChild(preNode);
                    }
                    else
                    {
                        log.warning("notNode was not son of it's father");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean dealMetaChar()
    {
        //FIXME 下面代码删除了所有meta character结点，需要将B结点改为可识别的Node
        Node node = head;
        Stack <Node>stack = new Stack<Node>();
        stack.push(head);

        while(!stack.empty())
        {
            node = stack.pop();
            if(node.nodeType() == NodeType.OPERATOR)
            {
                if(node.getLChild() != null) stack.push(node.getLChild());
                if(node.getRChild() != null) stack.push(node.getRChild());
                continue;
            }
            if(node.value instanceof MetaCharacter)
            {
                if(node == this.head)
                {
                    this.head = null;
                }
                else
                {
                    Node fatherNode = node.fatherNode;
                    if(node == fatherNode.getLChild())
                    {
                        fatherNode.setLChild(null);
                    }
                    else if(node == fatherNode.getRChild())
                    {
                        fatherNode.setRChild(null);
                    }
                    else
                    {
                        log.warning("notNode was not son of it's father");
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    //将生成的树转化为用数字表示，并生成一个charClass保存数字和字符的转换关系
    public boolean simplify()
    {
        if (!changeCharToInt()) return false;
        if (!dealNotNode()) return false;
        if (!dealMetaChar()) return false;

        HashSet<Node> singleCharNodes = new HashSet<Node>();  //保存单个字符的Node
        HashSet<Node> multiCharNodes = new HashSet<Node>();  //保存范围字符的Node

        HashSet<Integer> singleChars = new HashSet<Integer>();  //保存单个的字符
        HashSet<Integer[]> multiChars = new HashSet<Integer[]>();   //保存范围字符
        
        //int charClass[] = new int[65536];
        Stack<Node> stack = new Stack<Node>();

        Node node = this.head;

        Node lNode = null;
        Node rNode = null;

        stack.push(node);
        while(!stack.empty())
        {
            node = stack.pop();
            if (node.nodeType() == NodeType.OPERATOR)
            {
                lNode = node.getLChild();
                rNode = node.getRChild();
                if (lNode != null) stack.push(lNode);
                if (rNode != null) stack.push(rNode);
                continue;
            }
            if (node.nodeType() == NodeType.CHAR)
            {
                singleCharNodes.add(node);
                singleChars.add((int)node.value);
            }
            else if(node.nodeType() == NodeType.MULTICHARS)
            {
                multiCharNodes.add(node);
                multiChars.add(new Integer[]{(int)node.value, (int)node.value2});
            }
        }

        ArrayList<Integer[]> finalList = this.splitMulti(multiChars, singleChars);

        ArrayList<Node> finalNodes = new ArrayList<Node>();   //保存最终出现的字符结点

        //转换结点
        if(finalList.size() == 0)
        {
            log.warning("finalList's size is ZERO");
            return false;
        }

        //添加单结点
        for(Node sCN: singleCharNodes)
        {
            finalNodes.add(sCN);
        }

        for(Node mCN: multiCharNodes)
        {
            int s = 0;
            int e = finalList.size();
            int m = 0;
            //二分查找找到结点被分割后的起始结点
            while(s <= e)
            {
                m = (e + s) / 2;
                if (finalList.get(m)[0] > (int)mCN.value)
                {
                    e = m - 1;
                }
                else if(finalList.get(m)[0] < (int)mCN.value)
                {
                    s = m + 1;
                }
                else
                {
                    s = m;
                    break;
                }
            }
            if(finalList.get(s)[0] != mCN.value)
            {
                log.warning("finalList error: can't find " + finalList.get(s)[0]);
                return false;
            }

            e = s;
            while(finalList.get(e)[1] < (int)mCN.value2) e++;
            if(finalList.get(e)[1] != mCN.value2)
            {
                log.warning("finalList error: can't find " + finalList.get(e)[1]);
                return false;
            }

            //将数据转化为二叉树
            Node preNode = new Node(finalList.get(s)[0], finalList.get(s)[1]);
            preNode.setNodeType(NodeType.MULTICHARS);
            Node nextNode = null;
            Node optNode = null;

            finalNodes.add(preNode);
            for(int i = s + 1; i <= e; i++)
            {
                nextNode = new Node(finalList.get(i)[0], finalList.get(i)[1]);
                nextNode.setNodeType(NodeType.MULTICHARS);

                finalNodes.add(nextNode);

                optNode = new Node(Operator.OR);
                optNode.setLChild(preNode);
                optNode.setRChild(nextNode);
                preNode = optNode;
            }

            //将二叉树合并到原二叉树中
            if(mCN == this.head)
            {
                this.head = preNode;
            }
            else
            {
                Node fatherNode = mCN.fatherNode;
                if(fatherNode.getLChild() == mCN)
                {
                    fatherNode.setLChild(preNode);
                }
                else if(fatherNode.getRChild() == mCN)
                {
                    fatherNode.setRChild(preNode);
                }
                else
                {
                    log.warning("notNode was not son of it's father");
                    return false;
                }
            }
        } 

        //开始生成charClass并改为数字
        this.charClass = new int[65536]; //内部元素默认值为0
        for(int i = 0; i < finalList.size(); i++)
        {
            for(int j = finalList.get(i)[0]; j <= finalList.get(i)[1]; j++)
            {
                this.charClass[j] = i + 1;
            }
        }

        for(Node fN : finalNodes)
        {
            fN.num = this.charClass[(int)fN.value];
            fN.setNodeType(NodeType.CLASSNUM);
        }

        for(Integer[] fL : finalList)
        {
            System.out.println(fL[0] + " " + fL[1] + " " + this.charClass[fL[0]]);
        }

        return true;
    }
	
	private boolean createGrammerTree()
	{
		Node node = null;
		Node preNode = null;
		Node optNode = null;
        char c1, c2; //保存预读字符
		
		while (true)
		{
            char c = this.getNextC();
            boolean haveNode = false;
            node = null;

            if (c == '\0') break;

            //如果下一个是“\”符号且再下一个不是“\”符号的话则对\X进行操作
            c1 = this.getNextC();
            c2 = this.getNextC();
            if (c1 == '\\' && c2 != '\\')
            {
                this.backupC(c2);

                haveNode = true;

                if(c == 's')
                {
                    //node = DefaultNode.sNode.clone();
                    node = DefaultNode.getRangeTree(DefaultNode.s, false);
                }
                else if(c == 'b')
                {
                    node = new Node(MetaCharacter.B);
                }
                else if(c == 'w')
                {
                    //node = DefaultNode.wNode.clone();
                    node = DefaultNode.getRangeTree(DefaultNode.w, false);
                }
                else if(c == 'd')
                {
                    //node = DefaultNode.dNode.clone();
                    node = DefaultNode.getRangeTree(DefaultNode.d, false);
                }
                else if(c == 'S')
                {
                    //node = DefaultNode.SNode.clone();
                    node = DefaultNode.getRangeTree(DefaultNode.s, true);
                }
                else if(c == 'B')
                {
                    node = new Node(MetaCharacter.NB);
                }
                else if(c == 'W')
                {
                    //node = DefaultNode.WNode.clone();
                    node = DefaultNode.getRangeTree(DefaultNode.w, true);
                }
                else if(c == 'D')
                {
                    //node = DefaultNode.DNode.clone();
                    node = DefaultNode.getRangeTree(DefaultNode.d, true);
                }
                else
                {
                    node = new Node(c);
                }
            }
            else
            {
                this.backupC(c2);
                this.backupC(c1);
            }

            if (c == '^')
            {
                node = new Node(MetaCharacter.START);
                haveNode = true;
            }

            if (c == '$')
            {
                node = new Node(MetaCharacter.END);
                haveNode = true;
            }

			if (c == '(' )
			{
				if (resultStack.size() < 2) // 至少要有一个Node和一个右括号两个元素
				{
					head = null;
                    log.warning("缺少右括号或者括号内元素");
					return false;
				}
				
				//如果顶端是一个操作符的话说明字符串的左括号前面跟着操作符，不合法
                node = resultStack.pop();
				if(node.nodeType() == NodeType.OPERATOR && !node.isTree()) //这里包括了右括号的判断
				{
					this.head = null;
                    log.warning("小括号不匹配");
					return false;
				}
				
				while(!resultStack.empty())
				{
					preNode = resultStack.pop();

					if (preNode.nodeType() == NodeType.OPERATOR)
					{
						if(preNode.value == Operator.RP)    //遇到右括号说明匹配完成
						{
                            // 生成的树可能还需要与后面的节点或者操作符进行合并
                            haveNode = true; //XXX 加了一个flag，UGLY
							break;
						}
                        if(preNode.value == Operator.OR)
                        {
                            preNode = resultStack.pop();

                            // 在|符号后面不能跟着其他操作符。在其他代码正确无误情况下不可能进入这个if语句。
                            if(preNode.nodeType() == NodeType.OPERATOR && !preNode.isTree()) 
                            {
                                log.warning("operator append OR");
                                head = null;
                                return false;
                            }
                            
                            optNode = new Node(Operator.OR);
                            optNode.setLChild(node);
                            optNode.setRChild(preNode);
                            node = optNode;
                            continue;
                        }
						
						if (!preNode.isTree())
						{
							preNode.setLChild(node);
                            node = preNode;
							continue;
						}
					}
					optNode = new Node(Operator.AND);
					optNode.setLChild(node);
					optNode.setRChild(preNode);
                    node = optNode;
				}
				
				//如果没有产生节点说明括号不匹配，语法错误
				if (!haveNode)
				{
					head = null;
                    log.warning("不存在右括号");
					return false;
				}
			}
			
			if (c == ')')
			{
				optNode = new Node(Operator.RP);
				resultStack.push(optNode);
				continue;
			}
			
			//不允许单独获取到左中括号
			if (c == '[')
			{
                log.warning("获取到了单独的左中括号");
				this.head = null;
				return false;
			}
			
			if (c == ']')
			{
                haveNode = true; //在方括号内的都是有效结点
                
                HashSet<Character> oneChars = new HashSet<Character>();
                HashSet<char[]> multiChars = new HashSet<char[]>();
                ArrayList<Character> list = new ArrayList<Character>(); //保存括号内字符
                while(true)
                {
                    c = this.getNextC();
                    if (c == '\0')
                    {
                        break;
                    }

                    c1 = this.getNextC();

                    if (c1 == '\\')
                    {
                        list.add(c);
                        continue;
                    }
                    this.backupC(c1);

                    if(c == '[') break;
                    list.add(c);
                }
                if(c != '[')
                {
                    log.info("缺少括号：[");
                    head = null;
                    return false;
                }

                // if the first of character in [], we should set
                // the 
                boolean reverse = false;
                if(list.get(list.size()-1) == '^')
                {
                    reverse = true;
                }

                int j = 0;
                if(reverse)
                {
                    j = 1;
                }

                for(int i = 0; i < list.size() - j; i++)
                {
                    char n = list.get(i);
                    if(n == '-' || i >= list.size() - 1)
                    {
                        oneChars.add(n);
                        continue;
                    }
                    char n1 = list.get(i + 1);

                    //对于[1--9]这种情况当做[1-9]来处理
                    if(n1 == '-')
                    {
                        i += 1;
                        while(n1 == '-' && i < list.size()) n1 = list.get(++i);
                        // 如果已经搜索到结束都是“-”说明是[--a]这种格式
                        // 需要将a和-都存入单个字符中。
                        if(n1 == '-')
                        {
                            oneChars.add(n);
                            oneChars.add(n1);
                            break;
                        }
                        multiChars.add(new char[]{n1, n});
                        continue;
                    }
                    oneChars.add(n);
                }

                if(!oneChars.isEmpty())
                {
                    //获取set中的一个数据用于初始化node。
                    Iterator<Character> iter = oneChars.iterator();
                    c1 = iter.next();
                    node = new Node(c1);
                    oneChars.remove(c1);
                    for(char oneChar: oneChars)
                    {
                        preNode = new Node(oneChar);
                        optNode = new Node(Operator.OR);
                        optNode.setLChild(node);
                        optNode.setRChild(preNode);
                        node = optNode;
                    }
                }

                if(node == null && !multiChars.isEmpty())
                {
                    Iterator<char[]> iter = multiChars.iterator();
                    char []multiC = iter.next();
                    node = new Node(multiC[0], multiC[1]);
                    multiChars.remove(multiC);
                }
                for(char[] multiChar: multiChars)
                {
                    preNode = new Node(multiChar[0], multiChar[1]);
                    optNode = new Node(Operator.OR);
                    optNode.setLChild(node);
                    optNode.setRChild(preNode);
                    node = optNode;
                }

                if(reverse)
                {
                    Node notNode = new Node(Operator.NOT);
                    notNode.setLChild(node);
                    node = notNode;
                }
			}
			
			if (c == '{')
			{
                log.warning("获取到了单独的左大括号");
                this.head = null;
                return false;
			}
			
			if (c == '}')
			{
                int start = 0;  //起始位置。
                int end = 0;   //结束位置

                boolean flag = false; //标记是否有数字，如果没有的话设置end的值为-1，在start中采用默认的0即可
                for (int i = 1;; i*=10)
                {
                    c = this.getNextC();

                    if(c == '\0') break;

                    if(c == ',' || c == '{')
                    {
                        break;
                    }
                    if(!Character.isDigit(c))
                    {
                        log.warning("获取逗号失败");
                        head = null;
                        return false;
                    }
                    flag = true;
                    end += (c - '0') * i;
                }
                if (!flag)
                {
                    end = -1;
                }

                if(c == '\0')
                {
                    log.warning("表达式不完整");
                    head = null;
                    return false;
                }

                if(c == '{')
                {
                    optNode = new Node(end, end);
                    resultStack.push(optNode);
                    continue;
                }

                for(int i = 1;; i*=10)
                {
                    c = this.getNextC();
                    if (c == '\0') break;

                    if(c == '{')
                    {
                        break;
                    }
                    if(!Character.isDigit(c))
                    {
                        log.warning("大括号语法错误");
                        head = null;
                        return false;
                    }
                    start += (c - '0') * i;
                }
                
                if(c == '\0')
                {
                    log.warning("表达式不完整");
                    head = null;
                    return false;
                }

				optNode = new Node(start, end);
				resultStack.push(optNode);
				continue;
			}
			
			if (c == '|')
			{
				node = resultStack.get(resultStack.size() - 1);
				if(node.nodeType() == NodeType.OPERATOR && !node.isTree())
				{
                    log.warning("在|后面跟着其他操作符");
					this.head = null;
					return false;
				}

				optNode = new Node(Operator.OR);
				resultStack.push(optNode);
				continue;
			}
			if (c == '+')
			{
                if(!resultStack.empty())
                {
                    node = resultStack.get(resultStack.size() - 1);
                    if(node.nodeType() == NodeType.OPERATOR && !node.isTree())
                    {
                        this.head = null;
                        return false;
                    }
                }

				optNode = new Node(Operator.PLUS);
				resultStack.push(optNode);
				continue;
			}
			
			if (c == '*')
			{
                if(!resultStack.empty())
                {
                    node = resultStack.get(resultStack.size() - 1);
                    if(node.nodeType() == NodeType.OPERATOR && !node.isTree())
                    {
                        this.head = null;
                        return false;
                    }
                }

				optNode = new Node(Operator.STAR);
				resultStack.push(optNode);
				continue;
			}
			
			if (c == '?')
			{
                if(!resultStack.empty())
                {
                    node = resultStack.get(resultStack.size() - 1);
                    if(node.nodeType() == NodeType.OPERATOR && !node.isTree())
                    {
                        this.head = null;
                        return false;
                    }
                }

				optNode = new Node(Operator.QM);
				resultStack.push(optNode);
				continue;
			}

            if(!haveNode)
            {
                //运行到这里说明c为普通字符，将其转换为节点
                node = new Node(c); 
            }

			if(resultStack.empty())
			{
				resultStack.push(node);
				continue;
			}
			
			preNode = resultStack.pop();
			
			if(preNode.nodeType() == NodeType.OPERATOR && !preNode.isTree())
			{
				if (preNode.value == Operator.RP ||
                        preNode.value == Operator.OR)
				{
					resultStack.push(preNode);
					resultStack.push(node);
					continue;
				}
				if (!preNode.isTree())
				{
					preNode.setLChild(node);
					resultStack.push(preNode);
					continue;
				}
			}
            if (preNode.nodeType() == NodeType.RANGE)
            {
                preNode.setLChild(node);
                resultStack.push(preNode);
                continue;
            }
			optNode = new Node(Operator.AND);
			optNode.setLChild(node);
			optNode.setRChild(preNode);
			resultStack.push(optNode);
		}
		
        //输入栈中的数据处理完毕，接下来处理合并结果栈中的数据
		if (resultStack.empty())
		{
			this.head = null;
			return false;
		}
		
		//如果顶端是一个操作符的话说明字符串的第一个字符为操作符，不合法
        node = resultStack.pop();
		if(node.nodeType() == NodeType.OPERATOR && !node.isTree())
		{
			this.head = null;
			return false;
		}
		
		//对栈中剩余的结点（树）进行合并
		while (!resultStack.empty())
		{
			preNode = resultStack.pop();
			
			if (!preNode.isTree() && preNode.nodeType() == NodeType.OPERATOR)
			{
				if (preNode.value == Operator.RP)
				{
					this.head = null;
					return false;
				}
                if (preNode.value == Operator.OR)
                {
                    if(resultStack.empty())
                    {
                        log.warning("resultStack empty\nlast value:" + preNode);
                        head = null;
                        return false;
                    }
                    preNode = resultStack.pop();

                    if(preNode.nodeType() == NodeType.OPERATOR && !preNode.isTree()) 
                    {
                        log.warning("operator append OR");
                        head = null;
                        return false;
                    }

                    optNode = new Node(Operator.OR);
                    optNode.setLChild(node);
                    optNode.setRChild(preNode);
                    node = optNode;
                    continue;
                }
				
				if (!preNode.isTree())
				{
					preNode.setLChild(node);
                    node = preNode;
					continue;
				}
			}
				
			optNode = new Node(Operator.OR);
			optNode.setLChild(node);
			optNode.setRChild(preNode);
            node = optNode;
		}
		
		this.head = node;
		return true;
	}
	
	void showTree()
	{
		ArrayList<ArrayList<Node>> lists = new ArrayList<ArrayList<Node>>();
		ArrayList<Node> currentList = new ArrayList<Node>();
		currentList.add(this.head);
		lists.add(currentList);
		boolean flag = true;
		while(flag)
		{
			flag = false;
			currentList = new ArrayList<Node>();
			ArrayList<Node> lastList = lists.get(lists.size() - 1);
			for(Node node : lastList)
			{
				if (node != null)
				{
					currentList.add(node.getLChild());
					currentList.add(node.getRChild());
					if (node.getLChild() != null || node.getRChild() != null)
						flag = true;
				}
				else
				{
					currentList.add(null);
					currentList.add(null);
				}
			}
			
			if(flag)
			{
				lists.add(currentList);
			}
		}
		
		for(int i = 0; i < lists.size(); i++)
		{
			for(int j = 0; j < Math.pow(2, lists.size() - i - 1) - 1; j++)
				System.out.print("\t");
			for(Node node : lists.get(i))
			{
				if (node != null)
				{
					System.out.print(node + "\t");					
				}
				else
				{
					System.out.print("\t");
				}
				for(int j = 0; j < Math.pow(2, lists.size() - i) - 1; j++)
					System.out.print("\t");
			}
			System.out.println();
		}		
	}
}
