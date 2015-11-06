package pers.xia.jregexp.engine;

public class Edge
{
	int matchContent;
	Status start = null;
	Status end = null;

    //规定matchContent为-1时表示为E结点。
    Edge(int matchContent)
    {
        this.matchContent = matchContent;
    }
}
