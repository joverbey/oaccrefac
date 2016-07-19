int main() {
    int x = 1;
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        int v = 0;
        v = x; /*<<<<< 6,0,11,0,pass*/
        x = v;
        v++;
        x++;
        x = v + x;
    }
}
