/*
 * This test demonstrates that a refactoring can't happen if the loop is unrolled more
 * times than it is ran.
 */
int main() {

	char a[10];
    for (int i = 0; i < 10; i++) /*<<<<< 8,1,11,1,11,fail*/
        a[i] = '\0';
    return 0;
}
