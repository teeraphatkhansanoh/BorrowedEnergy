package borrowedenergy.model;

/** A complete stage occupies the whole viewport; layouts are authored independently. */
public final class Level {
    public static final int WIDTH = 36, HEIGHT = 20;
    private final char[][] tiles = new char[HEIGHT][WIDTH];
    private final int stageIndex;
    public Level() { this(0); }
    public Level(int stageIndex) {
        if (stageIndex<0 || stageIndex>2) throw new IllegalArgumentException("Unknown stage");
        this.stageIndex=stageIndex;
        for(int y=0;y<HEIGHT;y++) for(int x=0;x<WIDTH;x++)
            tiles[y][x]=x==0 || y==0 || x==WIDTH-1 || y==HEIGHT-1 ? '#' : '.';
        if(stageIndex==0) garden();else if(stageIndex==1) transit();else vault();
    }
    private void garden() {
        for(int y=1;y<19;y++) if(y!=9 && y!=10) tiles[y][18]='#';
        block(3,2,4,1,'#');block(11,2,3,1,'#');block(10,13,5,3,'~');block(3,15,3,1,'#');
        block(22,3,3,2,'#');block(27,7,3,1,'#');block(21,15,3,2,'~');block(29,15,2,1,'#');
    }
    private void transit() {
        for(int x=1;x<35;x++) {
            if(x!=9 && x!=10) tiles[7][x]='#';
            if(x!=26 && x!=27) tiles[13][x]='#';
        }
        block(7,2,3,1,'#');block(29,3,3,2,'#');block(17,8,3,2,'~');
        block(3,11,3,1,'#');block(12,15,3,2,'~');block(29,15,2,1,'#');
    }
    private void vault() {
        for(int y=1;y<19;y++) if(y!=5 && y!=6) tiles[y][12]='#';
        for(int x=13;x<35;x++) if(x!=28 && x!=29) tiles[12][x]='#';
        block(4,12,5,2,'~');block(2,7,2,2,'#');block(8,2,2,1,'#');block(21,6,3,4,'~');
        block(15,2,3,1,'#');block(29,2,4,1,'#');block(16,14,2,1,'#');block(19,17,2,1,'~');block(31,18,2,1,'#');
    }
    private void block(int sx,int sy,int w,int h,char value) {
        for(int y=sy;y<sy+h;y++) for(int x=sx;x<sx+w;x++) tiles[y][x]=value;
    }
    public char tileAt(int x,int y) { return x<0 || y<0 || x>=WIDTH || y>=HEIGHT ? '#' : tiles[y][x]; }
    public int stageIndex() { return stageIndex; }
    /** Retained for compatibility; a stage is no longer split into sectors. */
    public int sectorAt(double x) { return 0; }
}
