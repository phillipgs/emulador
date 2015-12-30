package beanes.sound;

import beanes.*;

/*
 *
 *
DMC
$4010   il-- ffff   playback mode, loop, frequency index
$4011   -ddd dddd   DAC
$4012   aaaa aaaa   sample address
$4013   llll llll   sample length
 *
value	clocks  octave  scale
-----	------  ------  -----
F	1B0	8	C
E	240	7	G
D	2A8	7	E
C	350	7	C
B	400	6	A
A	470	6	G
9	500	6	F
8	5F0	6	D
7	6B0	6	C
6	710	5	B
5	7F0	5	A
4	8F0	5	G
3	A00	5	F
2	AA0	5	E
1	BE0	5	D
0	D60	5	C

 */
public class DMChannel extends SoundChannel {
    
    public DMChannel(PAPU papu) {
        super(papu);
    }
    
    public void write(int address, short value) {
    }
}
