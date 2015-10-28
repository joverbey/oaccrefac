int main() {
    int a, b, c, d;
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) { /*<<<<< 4,1,10,1,fail*/
        a = 5;
        b = 6;
        c = 7;
        d = 8;
    }
}
