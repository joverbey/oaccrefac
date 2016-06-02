int main() {
    int x = 1;
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        int v = 0;
        x = v; /*<<<<< 6,0,7,0,pass*/
    }
}
