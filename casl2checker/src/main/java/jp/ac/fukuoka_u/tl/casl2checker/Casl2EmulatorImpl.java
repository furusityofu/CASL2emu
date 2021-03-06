package jp.ac.fukuoka_u.tl.casl2checker;

import jp.ac.fukuoka_u.tl.casl2emu.Casl2Emulator;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by furusho on 2017/01/10.
 */

public class Casl2EmulatorImpl extends Casl2Emulator {

    static final int KIRAKIRABOSHI_LENGTH =42;
    static char[] sound = new char[KIRAKIRABOSHI_LENGTH];
    static char[][] octangle = new char[8][4];
    static int soundindex=0;
    static int octangleindex=0;
    public static boolean inputflag=false;

    public int getOctangleindex() {
        return octangleindex;
    }

    public char[][] getOctangle() {
        return octangle;
    }
    public int getSoundindex() {
        return soundindex;
    }

    public char[] getsound() {
        return sound;
    }

    public void initializesound(){
        sound = new char[KIRAKIRABOSHI_LENGTH];
        soundindex=0;
    }


    public Casl2EmulatorImpl() {
        Arrays.fill(sound, (char) 0);
    }

    @Override
    public void opSVC(char cpc, short[] sr) {
                int wordCount;
        char[] instArray;
        char effective;
        short smember;
        int ians;
        char r1;
        char cmember;//データに基づいて処理する
        wordCount = 2;
        instArray = memory.getMemoryArray(register.getPc(), wordCount);
        effective = getEffectiveAddress();
        char memory_position;
        char count;
        char[] subarray;
        //spの指すアドレスを取得
        switch(effective){
            case 0xFF00://符号付き値input
                if(isRunflag()){
                    setRunflag(false);
                    setInterruptflag(true);
                }
                memory_position = 7;
//                inputintent.putExtra(context.getString(R.string.memory_position),memory_position);
//                inputintent.putExtra(context.getString(R.string.ValueType),0xFF00);
//                context.sendBroadcast(inputintent);
                break;
            case 0xFF01://符号なし値input
                if(isRunflag()){
                    setRunflag(false);
                    setInterruptflag(true);
                }
                inputflag=true;
                memory_position = 7;
//                inputintent.putExtra(context.getString(R.string.memory_position),memory_position);
//                inputintent.putExtra(context.getString(R.string.ValueType),0xFF01);
//                context.sendBroadcast(inputintent);
                break;
            case 0xFF02://input
                if(isRunflag()){
                    setRunflag(false);
                    setInterruptflag(true);
                }
                memory_position = register.getGr()[7];
                char length = register.getGr()[6];
//                inputintent.putExtra(context.getString(R.string.memory_position),memory_position);
//                inputintent.putExtra(context.getString(R.string.input_length),length);
//                inputintent.putExtra(context.getString(R.string.ValueType),0xFF02);
//                context.sendBroadcast(inputintent);
                break;

            case 0xFF03://OUT
                //r7を先頭アドレス、r6を文字数(wordの数ではない)とする。
                memory_position = register.getGr()[7];
                count = register.getGr()[6];
                //文字数分のデータを読み取りStringに変換。
                subarray = Arrays.copyOfRange(memory.getMemoryAll(),memory_position,memory_position+count);
                byte[] chardataStr = new byte[subarray.length];
                byte chardata = 0;
                for(int i =0;i<subarray.length;i++){

                    chardata = (byte) (subarray[i]&0x00FF);
                    if((chardata>=0x20&&chardata<=0x7E)||chardata==0x0a){
                       chardataStr[i]=chardata;
                    }
                }
                System.out.println(new String(chardataStr));
                //outputBuffer.addData(new String(chardataStr));
                break;
            case 0xFF10://算術乗算
                /**
                 * GR6:掛けられる数
                 * GR7:掛ける数
                 */
                sr[0] = (short) register.getGr()[7];
                smember = (short) register.getGr()[6];
                ians = (int) checkShortRange(sr[0]*smember);
                register.setGr((char) ((ians&0xFFFF0000)>>16),6);
                register.setGr((char) ((ians&0x0000FFFF)),7);
                break;
            case 0xFF11://論理乗算
                /**
                 * GR6:掛けられる数
                 * GR7:掛ける数
                 */
                r1 = register.getGr()[7];
                cmember = register.getGr()[6];
                ians = (int) checkCharRange(r1*cmember);
                register.setGr((char) ((ians&0xFFFF0000)>>16),6);
                register.setGr((char) ((ians&0x00FFFF)),7);
                break;
            case 0xFF12://算術除算
                /**
                 * GR6:割られる数
                 * GR7:割る数
                 */
                sr[0] = (short) register.getGr()[7];
                smember = (short) register.getGr()[6];
                ians = (short) checkShortRange(smember/sr[0]);
                int amari = smember%sr[0];
                register.setGr((char) ians,6);
                register.setGr((char) amari,7);
                break;
            case 0xFF13://論理除算
                /**
                 * GR6:割られる数
                 * GR7:割る数
                 */
                r1 = register.getGr()[7];
                cmember = register.getGr()[6];

                ians = (char) checkCharRange(cmember/r1);
                char amari1 = (char) (cmember%r1);
                register.setGr((char) ians,6);
                register.setGr(amari1,7);
                break;
            case 0xFF30://描画
                //先頭アドレス:gr7
                memory_position = register.getGr()[7];
                int color;
                int width;
                subarray = Arrays.copyOfRange(memory.getMemoryAll(),memory_position,memory_position+7);
                switch (subarray[0]){//種類別の処理
                    case 3://circle
                        float cx = (short)subarray[1];
                        float cy = (short)subarray[2];
                        float radius = (short)subarray[3];
                        float[] circleprop = {cx,cy,radius};
                        color = subarray[4];
                        //outputBuffer.addDrawObjectArray(1,circleprop,color,1);
                        break;
                    case 2://rectangle
                        int left = (short)subarray[1];
                        int top = (short)subarray[2];
                        int right = (short)subarray[3];
                        int bottom = (short)subarray[4];
//                        Rect rect = new Rect(left,top,right,bottom);
//                        color = subarray[5];
//                        outputBuffer.addDrawObjectArray(2,rect,color,1);
                        break;
                    case 1://line
                        if(octangleindex<8){
                            octangle[octangleindex][0]=subarray[1];
                            octangle[octangleindex][1]=subarray[2];
                            octangle[octangleindex][2]=subarray[3];
                            octangle[octangleindex][3]=subarray[4];
                            octangleindex++;
                        }
                        break;
                    case 0://point
                        float x=(short)subarray[1];
                        float y=(short)subarray[2];
                        float[]pp = {x,y};
                        color = subarray[3];
                        width = subarray[4];
                        //outputBuffer.addDrawObjectArray(4,pp,color,width);
                        break;
                    default:

                }
                break;
            case 0xFF40://音を鳴らす
                //先頭アドレス:gr7 データ数:gr6
                memory_position = register.getGr()[7];
                count = register.getGr()[6];
                int ontei=9;
                switch(memory_position){
                    case 60:
                        ontei = 0;
                        break;
                    case 62:
                        ontei = 1;
                        break;
                    case 64:
                        ontei = 2;
                        break;
                    case 65:
                        ontei = 3;
                        break;
                    case 67:
                        ontei = 4;
                        break;
                    case 69:
                        ontei = 5;
                        break;
                    case 71:
                        ontei = 6;
                        break;
                    default:
                        ontei = memory_position;
                }
                if(ontei<7){
                    if(soundindex< KIRAKIRABOSHI_LENGTH){
                        sound[soundindex]= (char) ontei;
                        soundindex++;
                    }
                }
                break;
            case 0xFF20://浮動小数点数演算
                //先頭アドレス:gr7
                //有効桁数7桁 指数部-37~37
                //仮数部は4*7=28ビットで表す(2word)符号は-の時8。指数部は1word使う。
                //演算の種類gr6
                memory_position = register.getGr()[7];
                char op = register.getGr()[6];
                subarray = Arrays.copyOfRange(memory.getMemoryAll(),memory_position,memory_position+6);
                char[] a_kasu = Arrays.copyOfRange(subarray,0,2);
                double a = getFloat(subarray[2], a_kasu);
                char[] b_kasu = Arrays.copyOfRange(subarray,3,5);
                double b = getFloat(subarray[5], b_kasu);
                char r_position = (char) (memory_position+6);
                float r;
                switch (op){
                    case 0x0://足し算
                        r=(float)checkFloatRange(a+b);
                        break;
                    case 0x1://引き算
                        r=(float)checkFloatRange(a-b);
                        break;
                    case 0x2://掛け算
                        r=(float)checkFloatRange(a*b);
                        break;
                    case 0x3://割り算
                        r=(float)checkFloatRange(a/b);
                        break;
                    case 0x4://べき乗
                        r=(float)checkFloatRange(Math.pow(a,b));
                        break;
                    case 0x5://正弦
                        r=(float)checkFloatRange(Math.sin((a/180)*Math.PI));
                        break;
                    case 0x6://余弦
                        r=(float)checkFloatRange(Math.cos((a/180)*Math.PI));
                        break;
                    case 0x7://正接
                        r=(float)checkFloatRange(Math.tan((a/180)*Math.PI));
                        break;
                    default:
                        r=(float)0;
                }

                char[] r_array = getFloatArray(r);
                memory.setMemoryArray(r_array,r_position);
                break;
            case 0xFF22://浮動小数点数変換
                //先頭アドレス:gr7
                //変換後代入先アドレス:gr6
                //仮数部は4*7=28ビットで表す(2word)。符号は-の時F。指数部は1word使う。
                //メモリアドレスgr6
                memory_position = register.getGr()[7];
                char tr_positon = register.getGr()[6];
                subarray = Arrays.copyOfRange(memory.getMemoryAll(),memory_position,memory_position+6);
                char[] a_kasu1 = Arrays.copyOfRange(subarray,0,2);
                float a1 = (float) getFloat(subarray[2], a_kasu1);
                memory.setMemory((char) (a1/1),tr_positon);
                break;
            case 0xFF14://rand
                memory_position = register.getGr()[7];
                short random_max = Short.MAX_VALUE;
                short random_min = Short.MIN_VALUE;
                Random random = new Random(System.currentTimeMillis());
                short randnum = (short) (random.nextInt(random_max - random_min + 1) + random_min);
                register.setGr((char)randnum,7);
                break;
            case 0xFF0C://timer
                memory_position = register.getGr()[7];
                char sleeptime = memory.getMemory(memory_position);
                try {
                    //Thread.sleep(sleeptime);
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case 0xFF70://非同期入力（ボタン）
                /**
                 * gr7:入力を受け取るアドレス
                 * gr6:1なら表示、0なら非表示
                 * gr5:1~4で表示するボタンを選択
                 * 押したら+1する。すべてのボタンで共通。
                 * 押したらボタンに応じて1,2,3,4をアドレスに代入する。
                 */
                memory_position = register.getGr()[7];
                int visibility;
//                visibility = register.getGr()[6]>0 ? Button.VISIBLE : Button.INVISIBLE;
//                int buttonnum = register.getGr()[5];
//                outputBuffer.setButtonconfig(buttonnum-1,visibility,memory_position);
                break;
            case 0xFFFE://プログラム終了
                break;

        }
        //FF00 FABCで文字出力できるようにする
        //実行アドレスに

        register.setPc((char) (cpc + wordCount));
    }

    @Override
    public void run(int interval) {

    }

    @Override
    public void waitEmu() {

    }
}
