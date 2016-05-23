int main() {
#pragma acc parallel loop
    for (int i = 0; i < 100; i++) {
        int x = 1;
        x++; /*<<<<< 5,0,6,0,fail*/
    }
}
