package pers.xia.jregexp.engine;


/*
 * 语法树的节点
 */
public class Node implements Cloneable
{
	Object value;
    Object value2;
	private boolean isOperator = false; //是否为操作符
    private boolean isRange = false;    //是否为重复次数范围
	private boolean isTree = false;     //是否为一颗树
    private boolean isMulti = false;    //是否为多可选项
    private boolean isReverse = false;  //数据是否反向（即除value外的字符才算匹配）
	private Node lChild = null;
	private Node rChild = null;

    //设置字符或者操作
	Node(Object value)
	{
		this.value = value;
		if(this.value instanceof Operator)
		{
			this.isOperator = true;
		}
	}

    //设置连续的字符结点或者范围结点
    Node(Object value, Object value2)
    {
        this.value = value;
        this.value2 = value2;
        if(this.value instanceof Character && this.value2 instanceof Character)
        {
            this.isMulti = true;
        }
        else if(this.value instanceof Integer && this.value2 instanceof Integer)
        {
            this.isRange = true;
            this.isOperator = true;
        }
    }

	
	Node getLChild()
	{
		return this.lChild;
	}
	
	boolean setLChild(Node node)
	{
		this.lChild = node;
		this.isTree = true;
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
		return true;
	}

    boolean isTree()
    {
        return this.isTree;
    }

    boolean isOperator()
    {
        return this.isOperator;
    }

    boolean isRange()
    {
        return this.isRange;
    }

    boolean isMulti()
    {
        return this.isMulti;
    }

    boolean isReverse()
    {
        return this.isReverse;
    }

    boolean setReverse(boolean isReverse)
    {
        this.isReverse = isReverse;
        return true;
    }

    public String toString()
    {
        if(this.isRange)
        {
            return "{" + String.valueOf(this.value) + "," + 
                String.valueOf(this.value2) + "}";
        }
        if(this.isMulti)
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
