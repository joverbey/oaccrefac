int main() {
    int x = 1;
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        x++; /*<<<<< 5,0,6,0,pass*/
    }
}
