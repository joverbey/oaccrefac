#include <time.h>
#include <stdio.h>

int main() {

    int a = 0;
    srand(time(NULL));
    int n = rand() % 20;
    for (int i = 0; i < n; i++) { /*<<<<< 9,1,11,6,20,fail*/
        a = 0;
    }
}
