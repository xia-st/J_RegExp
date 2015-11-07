package pers.xia.jregexp.engine;

import java.util.LinkedList;

public class Status
{
	LinkedList<Edge> inEdge = new LinkedList<Edge>();
	LinkedList<Edge> outEdge = new LinkedList<Edge>();
	boolean finalStatus;

    private boolean addInEdge(Edge edge)
    {
        this.inEdge.add(edge);
        return true;
    }

    private boolean addOutEdge(Edge edge)
    {
        this.outEdge.add(edge);
        return true;
    }

    Edge getOutEdge(int matchContent)
    {
        for(Edge edge : outEdge)
        {
            if(edge.matchContent == matchContent)
            {
                return edge;
            }
        }
        return null;
    }

    Edge getInEdge(int matchContent)
    {
        for(Edge edge : inEdge)
        {
            if(edge.matchContent == matchContent)
            {
                return edge;
            }
        }
        return null;
    }

    private boolean removeInEdge(Edge edge)
    {
        return this.inEdge.remove(edge);
    }

    private boolean removeOutEdge(Edge edge)
    {
        return this.outEdge.remove(edge);
    }

    boolean setFinalStatus(boolean finalStatus)
    {
        this.finalStatus = finalStatus;
        return true;
    }

    boolean isFinalStatus()
    {
        return this.finalStatus;
    }

    boolean connInEdge(Edge edge)
    {
        if(edge.end != null)
        {
            edge.end.disConnInEdge(edge);
        }
        edge.end = this;
        this.addInEdge(edge);
        return true;
    }

    boolean connOutEdge(Edge edge)
    {
        if(edge.start != null)
        {
            edge.start.disConnOutEdge(edge);
        }
        edge.start = this;
        this.addOutEdge(edge);
        return true;
    }

    boolean disConnInEdge(Edge edge)
    {
        if(!this.removeInEdge(edge))
        {
            return false;
        }
        edge.end = null;
        return true;
    }

    boolean disConnOutEdge(Edge edge)
    {
        if(!this.removeOutEdge(edge))
        {
            return false;
        }
        edge.start = null;
        return true;
    }

    boolean disConnAllOutEdge()
    {
        for(Edge oE : this.outEdge)
        {
            oE.start = null;
            oE.end.disConnInEdge(oE);
        }
        this.outEdge.clear();
        return true;
    }

    boolean disConnAllInEdge()
    {
        for(Edge iE : this.inEdge)
        {
            iE.end = null;
            iE.start.disConnOutEdge(iE);
        }
        this.inEdge.clear();
        return true;
    }

    boolean containInEdge(Edge edge)
    {
        return this.inEdge.contains(edge);
    }

    boolean containOutEdge(Edge edge)
    {
        return this.outEdge.contains(edge);
    }

    @SuppressWarnings("unchecked")
	LinkedList<Edge> getAllInEdge()      
    {
        return (LinkedList<Edge>)this.inEdge.clone();
    }

    @SuppressWarnings("unchecked")
    LinkedList<Edge> getAllOutEdge()
    {
        return (LinkedList<Edge>)this.outEdge.clone();
    }
}
