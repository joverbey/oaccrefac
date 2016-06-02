int main() {
    int x[10];
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        int v = 0;
        x[0] = v + x[0]; /*<<<<< 6,0,7,0,pass*/
    }
}
