package pers.xia.jregexp.engine;

import java.util.ArrayList;
import java.util.Stack;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GrammerTree
{
    private static final Logger log =
        Logger.getLogger(GrammerTree.class.getName());


    Stack<Character> inputStack = new Stack<Character>();
	Node head = null;
	Stack<Node> resultStack = new Stack<Node>();

    private char cur;
    private boolean haveNextC = false;

    char getNextC()
    {
        if(this.haveNextC)
        {
            this.haveNextC = false;
            return this.cur;
        }
        if(this.inputStack.empty())
        {
            return '\0';
        }
        return this.inputStack.pop();
    }

    boolean backupC(char c)
    { 
        if(this.haveNextC)
        {
            return false;
        }
        this.cur = c;
        this.haveNextC = true;
        return true;
    }

	GrammerTree(String input)
	{
        log.setLevel(Level.ALL);
		for (int i = 0; i < input.length(); i++)
		{
			inputStack.push(input.charAt(i));
		}
		if (!this.createGrammerTree())
		{
            log.warning("ERROR");
        }
	}
	
	private boolean createGrammerTree()
	{
		Node node = null;
		Node preNode = null;
		Node optNode = null;
		
        char c = this.getNextC();
		while (c != '\0')
		{
            boolean haveNode = false;
            node = null;

            //如果下一个是“\”符号且再下一个不是“\”符号的话则对\X进行操作
            if(!inputStack.empty() && 
                    inputStack.get(inputStack.size() - 1) == '\\')
            {
                //TODO 对\X 这种格式进行处理。
                inputStack.pop();

                haveNode = true;
                if(c == 's')
                {
                    node = DefaultNode.sNode.clone();
                }

                if(c == 'b')
                {
                }

                if(c == 'w')
                {
                    node = DefaultNode.wNode.clone();
                }

                if(c == 'd')
                {
                    node = DefaultNode.dNode.clone();
                }

                if(c == 'S')
                {
                    node = DefaultNode.SNode.clone();
                }

                if(c == 'B')
                {
                }

                if(c == 'W')
                {
                    node = DefaultNode.WNode.clone();
                }

                if(c == 'D')
                {
                    node = DefaultNode.DNode.clone();
                }
            }

            if (c == '^')
            {
                //TODO 匹配字符串开始
            }

            if (c == '$')
            {
                //TODO 匹配字符串结束
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
				if(node.isOperator() && !node.isTree()) //这里包括了右括号的判断
				{
					this.head = null;
                    log.warning("小括号不匹配");
					return false;
				}
				
				while(!resultStack.empty())
				{
					preNode = resultStack.pop();

					if (preNode.isOperator())
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
                            if(preNode.isOperator() && !preNode.isTree()) 
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
                while(!inputStack.empty())
                {
                    c = inputStack.pop();
                    if(c == '[') break;
                    list.add(c);
                }
                if(c != '[')
                {
                    log.info("缺少括号：[");
                    head = null;
                    return false;
                }

                for(int i = 0; i < list.size(); i++)
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
                    char c1 = iter.next();
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
                    char []c1 = iter.next();
                    node = new Node(c1[0], c1[1]);
                    multiChars.remove(c1);
                }
                for(char[] multiChar: multiChars)
                {
                    preNode = new Node(multiChar[0], multiChar[1]);
                    optNode = new Node(Operator.OR);
                    optNode.setLChild(node);
                    optNode.setRChild(preNode);
                    node = optNode;
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

                for (int i = 1;!inputStack.empty(); i*=10)
                {
                    c = inputStack.pop();
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
                    end += (c - '0') * i;
                }
                if(inputStack.empty())
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
                    c = inputStack.pop();
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
                //由于在{}表达式前面必须还要存在元素，所以可以假设必须不为空
                if(inputStack.empty())
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
				if(node.isOperator() && !node.isTree())
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
                    if(node.isOperator() && !node.isTree())
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
                    if(node.isOperator() && !node.isTree())
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
                    if(node.isOperator() && !node.isTree())
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
			
			if(!preNode.isTree() && preNode.isOperator())
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
		if(node.isOperator() && !node.isTree())
		{
			this.head = null;
			return false;
		}
		
		//对栈中剩余的结点（树）进行合并
		while (!resultStack.empty())
		{
			preNode = resultStack.pop();
			
			if (!preNode.isTree() && preNode.isOperator())
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

                    if(preNode.isOperator() && !preNode.isTree()) 
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
