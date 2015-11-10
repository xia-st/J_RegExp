package pers.xia.jregexp.engine;

import java.util.Stack;
import java.util.logging.Logger;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;


public class DFA
{
    private static final Logger log = Logger.getLogger(DFA.class.getName());

	Status startStatus = null;
	String input = null;
    int [] charClass = null;
    GrammerTree tree;
    int [][] dfa = null; //记录
    boolean []finalStatus = null;

    public DFA(String input)
    {
        this.input = input;
        this.createGrammerTree();
        this.createENFA();
        // this.showDFA(this.startStatus);
        this.createNFA();
        this.createDFA();
        this.createDfaTable();
        // this.showDFA(this.startStatus);
    }

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

	boolean createGrammerTree()
	{
        this.tree = new GrammerTree(this.input);
		// this.tree.showTree();
        this.tree.simplify();
		// this.tree.showTree();
        this.charClass = this.tree.charClass;
		return true;
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
            SES ses2 = aa.copySelf();
            ses2 = connWithChoose(ses2);
            Edge edge = new Edge(-1);
            ses2.startS.connInEdge(edge);
            ses2.endS.connOutEdge(edge);
            a = connWithAnd(a, ses2);
            // a.startS.connInEdge(edge);
            // a.endS.connOutEdge(edge);
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
        Status status = this.startStatus;
        Stack<Status> statusStack = new Stack<Status>(); //保存有效status的信息。
        HashMap<Status, LinkedList<Edge>> map = new HashMap<Status, LinkedList<Edge>>(); // 保存status与它的有效边。

        HashSet<Status> emptyStatus = new HashSet<Status>(); //inEdge值都为-1的status
        HashSet<Edge> emptyEdge = new HashSet<Edge>(); //值为-1的inEdge

        statusStack.push(status);

        HashSet<Status> checkedStatus = new HashSet<Status>();//保存在下面循环中已经处理过的status，防止重复访问
        //找到所有status能到达的第一个非空edge，结果存在map中。
        while(!statusStack.empty())
        {
            status = statusStack.pop(); //当前处理的结点

            //判断是否已经处理过，如果已经处理过的话进行下次循环
            if(checkedStatus.contains(status))
            {
                continue;
            }
            checkedStatus.add(status);

            Stack<Status> tempStack = new Stack<Status>(); //中间可能途径的结点
            tempStack.push(status);

            LinkedList<Edge> edges = new LinkedList<Edge>(); // 保存一个status第一个到达的非空edge

            HashSet<Edge> checkedEdges = new HashSet<Edge>(); //保存在下面循环中已经访问过的edge，防止重复访问
            //获取到status所有下一个非空的edge，将其与status放入map中。
            while(!tempStack.empty())
            {
                Status tempStatus = tempStack.pop();
                LinkedList<Edge> outEdgeList = tempStatus.getAllOutEdge();
                for(Edge oEL : outEdgeList)
                {
                    //判断oEL是否已经处理过，如果处理过的话继续进行循环。
                    if(checkedEdges.contains(oEL))
                    {
                        continue;
                    }
                    checkedEdges.add(oEL);

                    if(oEL.matchContent == -1)
                    {
                        emptyEdge.add(oEL);
                        if(oEL.end.isFinalStatus())
                        {
                            status.setFinalStatus(true);
                        }
                        tempStack.push(oEL.end);

                        //检查oEL.end的inEdge是否都为-1，如果是的话放入到emptyStatus中。
                        //如果在emptyStatus或者map的key中已经包含了这个status，就不需要继续进行判断了
                        if(oEL.end != this.startStatus &&
                                !emptyStatus.contains(oEL.end) &&
                                !map.keySet().contains(oEL.end))
                        {
                            LinkedList<Edge> inEdge = oEL.end.getAllInEdge();
                            boolean flag = false;
                            for(Edge iE : inEdge)
                            {
                                if(iE.matchContent != -1)
                                {
                                    flag = true;
                                    break;
                                }
                            }
                            if(!flag)
                            {
                                emptyStatus.add(oEL.end);
                            }
                        }
                    }
                    else
                    {
                        edges.add(oEL);
                        statusStack.push(oEL.end);
                    }
                }
            }
            map.put(status, edges);
        }


        //创建新edge，将status和选中的edge.end连接起来
        for(Map.Entry<Status, LinkedList<Edge>> mp : map.entrySet())
        {
            Status status2 = mp.getKey();
            for(Edge eg : mp.getValue())
            {
                if(eg.start == status2)
                {
                    continue;
                }
                Edge edge = new Edge(eg.matchContent);
                status2.connOutEdge(edge);
                eg.end.connInEdge(edge);
            }
        }

        //把空status和空edge删除
        //先删除空status，再删除edge
        for(Status eS : emptyStatus)
        {
            eS.disConnAllInEdge();
            eS.disConnAllOutEdge();
        }

        for(Edge eE : emptyEdge)
        {
            if(eE.start != null)
            {
                eE.start.disConnOutEdge(eE);
            }
            if(eE.end != null)
            {
                eE.end.disConnInEdge(eE);
            }
        }

		return true;
	}

    //判断两个HastSet中保存的数据是否一样
    boolean cmpStatusColl(HashSet<Status> hs1, HashSet<Status> hs2)
    {
        if(hs1.size() != hs2.size()) return false;
        if(!hs1.containsAll(hs2)) return false;
        return true;
    }

	boolean createDFA()
	{
        Status status = this.startStatus;
        HashMap<HashSet<Status>, Status> d = new HashMap<HashSet<Status>, Status>(); //已经存在的DFA状态
        Stack<HashSet<Status>> l = new Stack<HashSet<Status>>();        //未被处理但是已经创建的DFA状态

        HashSet<Status> statusSet = new HashSet<Status>(); //保存当前运算的节点集合
        statusSet.add(status);
        status = new Status();

        //需要对起始结点是否为结束结点进行判断
        if(this.startStatus.isFinalStatus())
        {
            status.setFinalStatus(true);
        }
        d.put(statusSet, status);
        l.push(statusSet);

        this.startStatus = status; //设置新的起始位置

        while(!l.isEmpty())
        {
            statusSet = l.pop();
            LinkedList<Edge> edgeList = new LinkedList<Edge>();

            //获取节点集合中的所有edge。
            for(Status sS : statusSet)
            {
                edgeList.addAll(sS.getAllOutEdge());
            }

            if(edgeList.isEmpty())
            {
                continue;
            }

            //对edge按照metchContent从小到达进行排序，方便接下来的操作
            Collections.sort(edgeList, new Comparator<Edge>()
            {

                @Override
                public int compare(Edge o1, Edge o2)
                {
                    return o1.matchContent - o2.matchContent;
                }
            });

            //将status集合按照edge的matchContent分组存放
            //nextStatusMap保存某个集合通过一个同matchContent的边能到达的所有Status的集合
            HashMap<Integer, HashSet<Status>> nextStatusMap = new HashMap<Integer, HashSet<Status>>();
            int matchContent = edgeList.get(0).matchContent;

            HashSet<Status> nextStatusSet = new HashSet<Status>();
            for(Edge eL : edgeList)
            {
                if(eL.matchContent == matchContent)
                {
                    nextStatusSet.add(eL.end);
                    continue;
                }
                nextStatusMap.put(matchContent, nextStatusSet);
                nextStatusSet = new HashSet<Status>();
                nextStatusSet.add(eL.end);
                matchContent = eL.matchContent;
            }
            nextStatusMap.put(matchContent, nextStatusSet);

            //将分组后的集合与已有的集合进行比较，如果存在的话将其与对应的Status进行链接，否则就创建一个新的Status并连接。
            for(Map.Entry<Integer, HashSet<Status>> nextStatus: nextStatusMap.entrySet())
            {
                boolean flag = false;

                for(HashSet<Status> existStatusSet : d.keySet())
                {
                    if(cmpStatusColl(existStatusSet, nextStatus.getValue()))
                    {
                        flag = true;

                        Edge newEdge = new Edge(nextStatus.getKey());
                        d.get(statusSet).connOutEdge(newEdge);
                        d.get(existStatusSet).connInEdge(newEdge);
                        break;
                    }
                }
                if(!flag)
                {
                    boolean isFinalStatus = false;

                    //判断是否为finalStatus：如果nextStatus中有一个结点是finalStatus，那么生成的结点必定为finalStatus
                    for(Status nSV : nextStatus.getValue())
                    {
                        if(nSV.isFinalStatus())
                        {
                            isFinalStatus = true;
                            break;
                        }
                    }

                    Status newStatus = new Status();
                    newStatus.setFinalStatus(isFinalStatus);

                    Edge newEdge = new Edge(nextStatus.getKey());
                    d.get(statusSet).connOutEdge(newEdge);
                    newStatus.connInEdge(newEdge);

                    d.put(nextStatus.getValue(), newStatus);
                    l.push(nextStatus.getValue());
                }
            }
        }

		return true;
	}

    boolean createDfaTable()
    {
        HashMap<Status, Integer> statusMap = new HashMap<Status, Integer>();
        int index = 0;
        LinkedList<Edge> edgeList = new LinkedList<Edge>();
        Status status = this.startStatus;

        Stack<Status> statusStack = new Stack<Status>();
        statusStack.push(status);
        statusMap.put(status, index++);

        int maxEdgeNum = 0; //保存edge最大的matchContent

        //获取到所有的status和edge
        while(!statusStack.empty())
        {
            status = statusStack.pop();
            LinkedList<Edge> edgeList2 = status.getAllOutEdge();
            edgeList.addAll(edgeList2);
            for(Edge eL : edgeList2)
            {
                if(maxEdgeNum < eL.matchContent)
                {
                    maxEdgeNum = eL.matchContent;
                }
                if(!statusMap.containsKey(eL.end))
                {
                    statusMap.put(eL.end, index++);
                    statusStack.push(eL.end);
                }
            }
        }

        this.finalStatus = new boolean[statusMap.size()];
        this.dfa = new int[statusMap.size()][maxEdgeNum + 1];

        //初始化
        for(int i = 0; i < dfa.length; i++)
        {
            for(int j = 0; j < dfa[i].length; j++)
            {
                dfa[i][j] = -1;
            }
        }

        //设置finsl status
        for(Map.Entry<Status, Integer>s : statusMap.entrySet())
        {
            if (s.getKey().isFinalStatus())
            {
                this.finalStatus[s.getValue()] = true;
            }
        }

        //设置dfa二维数组
        for(Edge eg : edgeList)
        {
            //设置每个坐标所示的下一个结点
            this.dfa[statusMap.get(eg.start)][eg.matchContent] = statusMap.get(eg.end);
        }

        return true;
    }

    boolean showDFA(Status startStatus)
    {
        HashSet<Status> set = new HashSet<Status>();
        Stack<Status> stack = new Stack<Status>();
        Status status = null;
        System.out.print(this.startStatus.hashCode());
        if(this.startStatus.isFinalStatus())
        {
            System.out.println(" Final Code");
        }
        else
        {
            System.out.println();
        }

        stack.push(this.startStatus);
        while(!stack.empty())
        {
            status = stack.pop();
            if(!set.contains(status))
            {
                set.add(status);

                LinkedList<Edge> outEdgeList = status.getAllOutEdge();
                for(Edge eL : outEdgeList)
                {
                    System.out.print(status.hashCode() + " -> " + eL.matchContent +
                          //  " " + eL.hashCode() +
                           " -> " + eL.end.hashCode());
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

    public int matchDfa(String input)
    {
        int lastFinalLength = -1;
        int status = 0;
        int index = 0;
        while(status != -1)
        {
            if(this.finalStatus[status])
            {
                lastFinalLength = index;
            }
            if(index >= input.length())
            {
                break;
            }
            status = dfa[status][this.charClass[input.charAt(index++)]];
        }
        return lastFinalLength;
    }

	// public static void main(String[] args)
	// {
        // String s = "\\d{6,11}";
        // DFA dfa = new DFA(s);
        // String input = "69426052618358332479";
        // int begin = 0;
        // int length = 0;
        // while(begin < input.length())
        // {
            // length = dfa.matchDfa(input.substring(begin));
            // if(length > 0)
            // {
                // System.out.println(input.substring(begin, begin + length));
                // begin += length;
            // }
            // else
            // {
                // begin++;
            // }
        // }
	// }
}
