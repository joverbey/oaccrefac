int main() {
    int a, b, c, d;
    for (int i = 0; i < 100; i++) { /*<<<<< 3,5,3,34,fail*/
        a = 5;
        b = 6;
    }
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        c = 7;
        d = 8;
    }
}
