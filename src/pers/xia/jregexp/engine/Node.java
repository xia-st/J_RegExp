package pers.xia.jregexp.engine;


/*
 * 语法树的节点
 */
public class Node implements Cloneable
{
	public Object value;
    public Object value2;
    public Node fatherNode;

	private boolean isTree = false;     //是否为一颗树
	private Node lChild = null;
	private Node rChild = null;
    private NodeType nodeType = null;
    
    //设置字符或者操作
	Node(Object value)
	{
		this.value = value;
		if(this.value instanceof Operator)
		{
            this.nodeType = NodeType.OPERATOR;
		}
        else
        {
            this.nodeType = NodeType.CHAR;
        }
	}

    //设置连续的字符结点或者范围结点
    Node(Object value, Object value2)
    {
        this.value = value;
        this.value2 = value2;
        if(this.value instanceof Character && this.value2 instanceof Character)
        {
            this.nodeType = NodeType.MULTICHARS;
        }
        else if(this.value instanceof Integer && this.value2 instanceof Integer)
        {
            this.nodeType = NodeType.RANGE;
        }
    }

    public boolean setNodeType(NodeType nodeType)
    {
        this.nodeType = nodeType;
        return true;
    }

	
	Node getLChild()
	{
        return this.lChild;
	}
	
	boolean setLChild(Node node)
	{
		this.lChild = node;
		this.isTree = true;
        node.fatherNode = this;
		return true;
	}
	
	Node getRChild()
	{
		return this.rChild;
	}
	
	boolean setRChild(Node node)
	{
		this.rChild = node;
		this.isTree = true;
        node.fatherNode = this;
		return true;
	}

    boolean isTree()
    {
        return this.isTree;
    }

    NodeType nodeType()
    {
        return this.nodeType;
    }

    public String toString()
    {
        if(this.nodeType == NodeType.RANGE)
        {
            return "{" + String.valueOf(this.value) + "," + 
                String.valueOf(this.value2) + "}";
        }
        if(this.nodeType == NodeType.MULTICHARS)
        {
            return value + "-" + value2;
        }
        return this.value.toString();
    }

    public Node clone()
    {
         try {   
            return (Node)super.clone();   
        } catch (CloneNotSupportedException e) {   
            return null;   
        }   
    }
}
