package pers.xia.jregexp.engine;

import java.util.Stack;
import java.util.logging.Logger;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.HashMap;


public class DFA
{
    private static final Logger log = Logger.getLogger(DFA.class.getName());

	Status startStatus = null;
	String input = null;
    int [] charClass = null;
    GrammerTree tree;
    
    //used to create a data structure like Status->Edge->Status
    static class SES
    {
        Status startS; //start status
        Status endS; //end status

        SES()
        {
            this.startS = this.endS = null;
        }

        SES(Status startS, Status endS)
        {
            this.startS = startS;
            this.endS = endS;
        }

        SES(int matchContent)
        {
            Edge edge = new Edge(matchContent);
            this.startS = new Status();
            this.endS = new Status();
            this.startS.connOutEdge(edge);
            this.endS.connInEdge(edge);
            //this.endS.setFinalStatus(true);
        }

        SES copySelf()
        {
            HashMap<Status, Status> map = new HashMap<Status, Status>(); //保存旧Status与新的Status的对应关系
            LinkedList<Edge> list = new LinkedList<Edge>(); //保存所有出现的edge
            LinkedList<Edge> list2 = null; //临时变量
            Status status = this.startS;
            Status status2 = null; //临时变量
            Stack<Status> stack= new Stack<Status>();
            stack.push(status);
            while(!stack.empty())
            {
                status = stack.pop();
                if(!map.containsKey(status))
                {
                    status2 = new Status();
                    map.put(status, status2);

                    list2 = status.getAllOutEdge();
                    list.addAll(list2);
                    for(Edge edge : list2)
                    {
                        stack.push(edge.end);
                    }
                }
            }

            Edge edge2 = null;
            for(Edge edge : list)
            {
                edge2 = new Edge(edge.matchContent);
                map.get(edge.start).connOutEdge(edge2);
                map.get(edge.end).connInEdge(edge2);
            }
            return new SES(map.get(this.startS), map.get(this.endS));
        }
    }
	
    public DFA(String input)
    {
        this.input = input;
        this.createGrammerTree();
        this.createENFA();
        this.showDFA(this.startStatus);
    }
	
	Node createGrammerTree()
	{
        this.tree = new GrammerTree(this.input);
		this.tree.showTree();
        this.tree.simplify();
		this.tree.showTree();
        this.charClass = this.tree.charClass;
		return null;
	}

    SES connWithAnd(SES a, SES b)
    {
        Edge edge = new Edge(-1);
        a.endS.connOutEdge(edge);
        b.startS.connInEdge(edge);
        return new SES(a.startS, b.endS);
    }

    SES connWithOr(SES a, SES b)
    {
        Edge edge1 = new Edge(-1);
        Edge edge2 = new Edge(-1);
        a.startS.connOutEdge(edge1);
        b.startS.connInEdge(edge1);
        a.endS.connInEdge(edge2);
        b.endS.connOutEdge(edge2);
        return a;
    }

    SES connWithRepeat(SES a, int s, int e)
    {
        SES aa = a.copySelf(); //a的一个备份，下面的代码都从这里复制a
        if(s == 0)
        {
            a = connWithChoose(a);
        }
        else if(s > 1)
        {
            //添加前几个结点
            SES ses2 = null;
            for(int i = 1; i < s; i++)
            {
                ses2 = aa.copySelf();
                a = connWithAnd(a, ses2);
            }
        }

        if(e == -1)
        {
            Edge edge = new Edge(-1);
            a.startS.connInEdge(edge);
            a.endS.connOutEdge(edge);
            return a;
        }

        SES ses2 = null;
        for(int i = 0; i < e - s; i++)
        {
            ses2 = aa.copySelf();
            ses2 = connWithChoose(ses2);
            a = connWithAnd(a, ses2);
        }
        return a;
    }

    SES connWithChoose(SES a)
    {
        Edge edge = new Edge(-1);
        a.startS.connOutEdge(edge);
        a.endS.connInEdge(edge);
        return a;
    }

	boolean createENFA()
	{
        Node node = this.tree.head;
        Stack<Node> tempNodeStack = new Stack<Node>();
        Stack<Node> nodeStack = new Stack<Node>();

        //把语法树转化为后序序列放到stack中。
        tempNodeStack.push(node);
        while(!tempNodeStack.empty())
        {
            node = tempNodeStack.pop();
            nodeStack.push(node);
            if(node.getLChild() != null) tempNodeStack.push(node.getLChild());
            if(node.getRChild() != null) tempNodeStack.push(node.getRChild());
        }

        if(nodeStack.empty())
        {
            log.warning("node stack is empty");
            return false;
        }

        Stack<SES> sesStack = new Stack<SES>();
        SES ses = null;
        SES ses2 = null;

        while(!nodeStack.empty())
        {
            node = nodeStack.pop();
            if(node.nodeType() == NodeType.CLASSNUM)
            {
                ses = new SES(node.num);
                sesStack.push(ses);
                continue;
            }
            if(node.nodeType() == NodeType.RANGE)
            {
                ses = sesStack.pop();
                ses = this.connWithRepeat(ses, (int)node.value, (int)node.value2);
                sesStack.push(ses);
                continue;
            }
            if(node.nodeType() == NodeType.OPERATOR)
            {
                if(node.value == Operator.AND)
                {
                    ses2 = sesStack.pop();
                    ses = sesStack.pop();
                    ses = this.connWithAnd(ses, ses2);
                    sesStack.push(ses);
                }
                else if(node.value == Operator.OR)
                {
                    ses2 = sesStack.pop();
                    ses = sesStack.pop();
                    ses = this.connWithOr(ses, ses2);
                    sesStack.push(ses);
                }
                else if(node.value == Operator.PLUS)
                {
                    ses = sesStack.pop();
                    ses = this.connWithRepeat(ses, 1, -1);
                    sesStack.push(ses);
                }
                else if(node.value == Operator.STAR)
                {
                    ses = sesStack.pop();
                    ses = this.connWithRepeat(ses, 0, -1);
                    sesStack.push(ses);
                }
                else if(node.value == Operator.QM)
                {
                    ses = sesStack.pop();
                    ses = this.connWithChoose(ses);
                    sesStack.push(ses);
                }
                else
                {
                    log.warning("Exist unsolved operator: " + 
                            ses.toString());
                    return false;
                }
                continue;
            }
            log.warning("Exist unsolved node: " + node.toString());
            return false;
        }
        if(sesStack.size() != 1)
        {
            log.warning("SES stack's size is not 1");
            return false;
        }
        ses = sesStack.pop();
        this.startStatus = ses.startS;
        ses.endS.setFinalStatus(true);
		return true;
	}

	boolean createNFA()
	{
        //TODO
		return true;
	}
	
	boolean createDFA()
	{
        //TODO
		return true;
	}

    boolean showDFA(Status startStatus)
    {
        HashSet<Status> set = new HashSet<Status>();
        Stack<Status> stack = new Stack<Status>();
        Status status = null;
        stack.push(startStatus);
        while(!stack.empty())
        {
            status = stack.pop();
            if(!set.contains(status))
            {
                set.add(status);
                LinkedList<Edge> outEdgeList = status.getAllOutEdge();
                for(Edge eL : outEdgeList)
                {
                    System.out.print(status.hashCode() + " -> " + eL.matchContent + " -> " + eL.end.hashCode());
                    if(eL.end.isFinalStatus())
                    {
                        System.out.println(" Final Code");
                    }
                    else
                    {
                        System.out.println();
                    }
                    stack.push(eL.end);
                }
                System.out.println();
            }
        }
        return true;
    }
	
	public static void main(String[] args)
	{
        String s = ".[b-f]";
        DFA dfa = new DFA(s);
	}
}
