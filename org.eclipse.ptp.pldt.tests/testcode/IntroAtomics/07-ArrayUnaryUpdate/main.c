int main() {
    int x[10];
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        x[0]++; /*<<<<< 5,0,6,0,pass*/
    }
}
