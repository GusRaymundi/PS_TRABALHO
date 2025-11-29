package executor;

public interface Opcode {

    int LDA   = 0x00; // A ← (m..m+2)
    int LDX   = 0x04; // X ← (m..m+2)
    int LDL   = 0x08; // L ← (m..m+2)
    int STA   = 0x0C; // m..m+2 ← (A)
    int STX   = 0x10; // m..m+2 ← (X)
    int STL   = 0x14; // m..m+2 ← (L)

    int ADD   = 0x18; // A ← A + M
    int SUB   = 0x1C; // A ← A - M
    int MUL   = 0x20; // A ← A * M
    int DIV   = 0x24; // A ← A / M
    int COMP  = 0x28; // A : M (CC)
    int TIX   = 0x2C; // X ← X+1; X : M (CC)

    int JEQ   = 0x30; // if CC = 0
    int JGT   = 0x34; // if CC > 0
    int JLT   = 0x38; // if CC < 0
    int J     = 0x3C; // salto incondicional

    int AND   = 0x40; // A ← A & M
    int OR    = 0x44; // A ← A | M
    int JSUB  = 0x48; // L ← PC; PC ← m
    int LDCH  = 0x50; // byte → A (byte direito)
    int STCH  = 0x54; // A(byte direito) → m

    int LDB   = 0x68; // B ← (m..m+2)
    int LDS   = 0x6C; // S ← (m..m+2)
    int LDT   = 0x74; // T ← (m..m+2)
    int STB   = 0x78; // m..m+2 ← (B)
    int STS   = 0x7C; // m..m+2 ← (S)
    int STT   = 0x84; // m..m+2 ← (T)
    int RSUB  = 0x4C; // PC ← L

    int ADDR  = 0x90; // r2 ← r2 + r1
    int SUBR  = 0x94; // r2 ← r2 - r1
    int MULR  = 0x98; // r2 ← r2 * r1
    int DIVR  = 0x9C; // r2 ← r2 / r1
    int COMPR = 0xA0; // r1 : r2 (CC)

    int SHIFTL = 0xA4; // r1 << n
    int SHIFTR = 0xA8; // r1 >> n (lógico)
    int RMO    = 0xAC; // r2 ← r1
    int CLEAR  = 0xB4; // r1 ← 0
    int TIXR   = 0xB8; // X ← X+1; X : r1 (CC)
}
