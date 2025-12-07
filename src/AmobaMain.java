import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AmobaMain {
    public static void main(String[] args) {
        new Game().start(args);
    }
}

class Game {
    private Board board;
    private final Scanner sc = new Scanner(System.in);
    private final char HUMAN = 'x';
    private final char AI = 'o';
    private final RandomAI ai = new RandomAI();

    public void start(String[] args) {
        int rows = 10, cols = 10;
        if (args.length >= 2) {
            try {
                rows = Integer.parseInt(args[0]);
                cols = Integer.parseInt(args[1]);
            } catch (Exception ignored) {}
        }
        if (cols < 5 || rows < 5 || cols > rows || rows > 25) {
            System.out.println("Érvénytelen méret; alapértelmezett 10x10 lesz (5<=M<=N<=25)");
            rows = 10; cols = 10;
        }
        board = new Board(rows, cols);

        String defaultFile = "board.txt";
        if (Files.exists(Path.of(defaultFile))) {
            try {
                board.loadFromFile(defaultFile);
                System.out.println("Betöltve: " + defaultFile);
            } catch (IOException e) {
                System.out.println("Hiba a fájl betöltésénél, üres tábla indul.");
            }
        } else {
            System.out.println("Nincs 'board.txt' — új üres tábla jön létre.");
        }

        System.out.println("Amőba (4-et kell egymás mellé rakni) — human: x (te), gép: o");
        System.out.println("Parancsok: pl. b3 (oszlopbetű + sorszám), save <fajl>, load <fajl>, quit, help");
        board.print();

        boolean humanTurn = true;
        while (true) {
            if (humanTurn) {
                System.out.print("Lépés (Te - x): ");
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("quit")) { System.out.println("Kilépés..."); break; }
                if (line.equalsIgnoreCase("help")) { printHelp(); continue; }
                if (line.toLowerCase().startsWith("save")) {
                    String[] sp = line.split("\\s+",2);
                    String f = sp.length>1?sp[1] : defaultFile;
                    try { board.saveToFile(f); System.out.println("Mentve: "+f);} catch(IOException e){System.out.println("Mentési hiba: "+e.getMessage());}
                    continue;
                }
                if (line.toLowerCase().startsWith("load")) {
                    String[] sp = line.split("\\s+",2);
                    String f = sp.length>1?sp[1] : defaultFile;
                    try { board.loadFromFile(f); System.out.println("Betöltve: "+f); board.print(); } catch(IOException e){System.out.println("Betöltési hiba: "+e.getMessage());}
                    continue;
                }

                Move m = parseMove(line);
                if (m == null) { System.out.println("Érvénytelen formátum. Példa: b3"); continue; }
                if (!board.isInside(m.r, m.c)) { System.out.println("A mező a táblán kívül van."); continue; }
                if (!board.isEmpty(m.r, m.c)) { System.out.println("A mező már foglalt."); continue; }
                try {
                    board.place(m.r, m.c, HUMAN);
                } catch (IllegalArgumentException ex) {
                    System.out.println("Érvénytelen lépés: " + ex.getMessage());
                    continue;
                }
                board.print();
                if (board.checkWin(HUMAN)) { System.out.println("Gratulálok — nyertél!"); break; }
                humanTurn = false;
            } else {
                System.out.println("Gép lép (o)...");
                List<Move> legal = board.getLegalMoves();
                if (legal.isEmpty()) { System.out.println("Nincs több lépés — döntetlen."); break; }
                Move m = ai.pick(legal);
                board.place(m.r, m.c, AI);
                System.out.println("Gép lép: " + toHumanMove(m));
                board.print();
                if (board.checkWin(AI)) { System.out.println("A gép nyert. Better luck next time."); break; }
                humanTurn = true;
            }

            if (board.isFull()) { System.out.println("A tábla tele — döntetlen."); break; }
        }
        System.out.println("Játék vége. Köszi hogy játszottál!");
    }

    private void printHelp() {
        System.out.println("Formátum: oszlopbetű (a...), sor szám (1..N). Például: b3 vagy j10");
        System.out.println("Első lépésnek a középső mező egyikét kell választani (kötelező).");
        System.out.println("Lépés csak akkor érvényes, ha legalább egy meglévő jelhez érintkezik (8 irányban).");
    }

    private Move parseMove(String s) {
        s = s.toLowerCase().replaceAll("\\\\s+","");
        if (s.length()<2) return null;
        int i=0; while (i<s.length() && Character.isLetter(s.charAt(i))) i++;
        if (i==0) return null;
        String colStr = s.substring(0,i);
        String rowStr = s.substring(i);
        int col = 0;
        for (char ch: colStr.toCharArray()) {
            if (ch<'a' || ch>'z') return null;
            col = col*26 + (ch - 'a');
        }
        int row;
        try { row = Integer.parseInt(rowStr)-1; } catch (Exception e) { return null; }
        return new Move(row, col);
    }

    private String toHumanMove(Move m) {
        int c = m.c;
        StringBuilder sb = new StringBuilder();
        sb.append((char)('a'+c));
        sb.append(m.r+1);
        return sb.toString();
    }
}

class Board {
    private final int rows, cols;
    private final char[][] a;
    private final char EMPTY = '.';

    public Board(int rows, int cols) {
        this.rows = rows; this.cols = cols;
        a = new char[rows][cols];
        for (int i=0;i<rows;i++) Arrays.fill(a[i], EMPTY);
    }

    public void print() {
        System.out.print("   ");
        for (int c=0;c<cols;c++) System.out.print((char)('a'+c) + " ");
        System.out.println();
        for (int r=0;r<rows;r++){
            System.out.printf("%2d ", r+1);
            for (int c=0;c<cols;c++) System.out.print(a[r][c] + " ");
            System.out.println();
        }
    }

    public boolean isInside(int r,int c){ return r>=0 && r<rows && c>=0 && c<cols; }
    public boolean isEmpty(int r,int c){ return isInside(r,c) && a[r][c]==EMPTY; }
    public boolean isFull(){ for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) if (a[r][c]==EMPTY) return false; return true; }

    public void place(int r,int c,char player) {
        if (!isInside(r,c)) throw new IllegalArgumentException("A mező nincs a táblán.");
        if (a[r][c]!=EMPTY) throw new IllegalArgumentException("A mező foglalt.");
        boolean first = isBoardEmpty();
        if (first) {
            if (!isCenter(r,c)) throw new IllegalArgumentException("Az első jelnek középen kell lennie.");
            a[r][c]=player; return;
        }
        boolean ok=false;
        for (int dr=-1;dr<=1;dr++) for (int dc=-1;dc<=1;dc++){
            if (dr==0 && dc==0) continue;
            int nr=r+dr, nc=c+dc;
            if (isInside(nr,nc) && a[nr][nc]!=EMPTY) ok=true;
        }
        if (!ok) throw new IllegalArgumentException("A lépésnek érintkeznie kell legalább egy meglévő jelhez.");
        a[r][c]=player;
    }

    private boolean isBoardEmpty(){ for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) if (a[r][c]!=EMPTY) return false; return true; }

    private boolean isCenter(int r,int c){
        int midr = (rows-1)/2; int midc = (cols-1)/2;
        if (rows%2==1 && cols%2==1) return r==midr && c==midc;
        for (int dr=0;dr<=1;dr++) for (int dc=0;dc<=1;dc++){
            int rr = midr+dr; int cc = midc+dc;
            if (isInside(rr,cc) && rr==r && cc==c) return true;
        }
        return false;
    }

    public List<Move> getLegalMoves(){
        List<Move> res = new ArrayList<>();
        boolean empty = isBoardEmpty();
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) if (a[r][c]==EMPTY){
            if (empty) { if (isCenter(r,c)) res.add(new Move(r,c)); }
            else {
                boolean ok=false;
                for (int dr=-1;dr<=1;dr++) for (int dc=-1;dc<=1;dc++){
                    if (dr==0 && dc==0) continue;
                    int nr=r+dr, nc=c+dc;
                    if (isInside(nr,nc) && a[nr][nc]!=EMPTY) ok=true;
                }
                if (ok) res.add(new Move(r,c));
            }
        }
        return res;
    }

    public boolean checkWin(char player){
        int need = 4;
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) if (a[r][c]==player){
            if (countDir(r,c,1,0,player) >= need) return true;
            if (countDir(r,c,0,1,player) >= need) return true;
            if (countDir(r,c,1,1,player) >= need) return true;
            if (countDir(r,c,1,-1,player) >= need) return true;
        }
        return false;
    }

    private int countDir(int r,int c,int dr,int dc,char player){
        int cnt=0; int rr=r, cc=c;
        while (isInside(rr,cc) && a[rr][cc]==player){ cnt++; rr+=dr; cc+=dc; }
        return cnt;
    }

    public void saveToFile(String filename) throws IOException{
        try(BufferedWriter w = Files.newBufferedWriter(Path.of(filename))){
            w.write(rows+" "+cols); w.newLine();
            for (int r=0;r<rows;r++){
                for (int c=0;c<cols;c++) w.write(a[r][c]);
                w.newLine();
            }
        }
    }

    public void loadFromFile(String filename) throws IOException{
        List<String> lines = Files.readAllLines(Path.of(filename));
        if (lines.isEmpty()) throw new IOException("Üres fájl");
        String[] hdr = lines.get(0).trim().split("\\s+");
        int r = Integer.parseInt(hdr[0]);
        int c = Integer.parseInt(hdr[1]);
        if (r!=rows || c!=cols) throw new IOException("A fájl mérete nem egyezik a tábláéval (vár: " + rows+"x"+cols+")");
        for (int i=0;i<rows;i++){
            String line = lines.get(i+1);
            for (int j=0;j<cols && j<line.length();j++) a[i][j] = line.charAt(j);
            for (int j=line.length(); j<cols; j++) a[i][j] = EMPTY;
        }
    }
}

class Move { public final int r,c; public Move(int r,int c){this.r=r;this.c=c;} }

class RandomAI {
    private final Random rnd = new Random();
    public Move pick(List<Move> legal){ return legal.get(rnd.nextInt(legal.size())); }
}
