// Differential Cryptanalysis of FEAL-4
// Uses a chosen-plaintext attack to fully recover the key
// For use with tutorial at http://theamazingking.com/crypto-feal.php

#include <time.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <stdint.h>

#define MAX_CHOSEN_PAIRS 10000
#define ROTATE_LEFT(x, n) (((x) << (n)) | ((x) >> (32-(n))))

int32_t winner = 0;
int32_t loser = 0;
uint32_t subkey[6];

uint8_t rotl2(uint8_t a) {
  return ((a << 2) | (a >> 6));
}

uint32_t leftHalf(uint64_t a) {
  return (a >> 32LL);
}

uint32_t rightHalf(uint64_t a) {
  return a;
}

uint8_t sepByte(uint32_t a, uint8_t index) {
  return a >> (8 * index);
}

uint32_t combineBytes(uint8_t b3, uint8_t b2, uint8_t b1, uint8_t b0) {
  return b3 << 24L | (b2 << 16L) | (b1 << 8L) | b0;
}

uint64_t combineHalves(uint32_t leftHalf, uint32_t rightHalf) {
  return (((uint64_t) (leftHalf)) << 32LL) | (((uint64_t) (rightHalf)) &
                                               0xFFFFFFFFLL);
}

uint8_t gBox(uint8_t a, uint8_t b, uint8_t mode) {
  return rotl2(a + b + mode);
}

uint32_t fBox(uint32_t plain) {
  uint8_t x0 = sepByte(plain, 0);
  uint8_t x1 = sepByte(plain, 1);
  uint8_t x2 = sepByte(plain, 2);
  uint8_t x3 = sepByte(plain, 3);
  uint8_t t0 = (x2 ^ x3);
  uint8_t y1 = gBox(x0 ^ x1, t0, 1);
  uint8_t y0 = gBox(x0, y1, 0);
  uint8_t y2 = gBox(t0, y1, 0);
  uint8_t y3 = gBox(x3, y2, 1);
  return combineBytes(y3, y2, y1, y0);
}

uint64_t encrypt(uint64_t plain) {
  uint32_t left = leftHalf(plain);
  uint32_t right = rightHalf(plain);
  left = left ^ subkey[4];
  right = right ^ subkey[5];
  uint32_t round2Left = left ^ right;
  uint32_t round2Right = left ^ fBox(round2Left ^ subkey[0]);
  uint32_t round3Left = round2Right;
  uint32_t round3Right = round2Left ^ fBox(round2Right ^ subkey[1]);
  uint32_t round4Left = round3Right;
  uint32_t round4Right = round3Left ^ fBox(round3Right ^ subkey[2]);
  uint32_t cipherLeft = round4Left ^ fBox(round4Right ^ subkey[3]);
  uint32_t cipherRight = cipherLeft ^ round4Right;
  return combineHalves(cipherLeft, cipherRight);
}

void generateSubkeys(int32_t seed) {
  srand(seed);
  for (int32_t c = 0; c < 6; c++)
    subkey[c] = (rand() << 16L) | (rand() & 0xFFFFL);
}

int32_t numPlain;
uint64_t plain0[MAX_CHOSEN_PAIRS];
uint64_t cipher0[MAX_CHOSEN_PAIRS];
uint64_t plain1[MAX_CHOSEN_PAIRS];
uint64_t cipher1[MAX_CHOSEN_PAIRS];

void undoFinalOperation() {
  for (int32_t c = 0; c < numPlain; c++) {
    uint32_t cipherLeft0 = leftHalf(cipher0[c]);
    uint32_t cipherRight0 = rightHalf(cipher0[c]) ^ cipherLeft0;
    uint32_t cipherLeft1 = leftHalf(cipher1[c]);
    uint32_t cipherRight1 = rightHalf(cipher1[c]) ^ cipherLeft1;
    cipher0[c] = combineHalves(cipherLeft0, cipherRight0);
    cipher1[c] = combineHalves(cipherLeft1, cipherRight1);
  }
}

uint32_t crackLastRound(uint32_t outdiff) {
  printf("  Using output differential of 0x%08x\n", outdiff);
  printf("  Cracking...");
  for (uint32_t fakeK = 0x00000000L; fakeK < 0xFFFFFFFFL; fakeK++) {/* loop1outer */
    int32_t score = 0;
    for (int32_t c = 0; c < numPlain; c++)/* loop1inner */
      uint32_t cipherLeft = (cipher0[c] >> 32LL);
      cipherLeft ^= (cipher1[c] >> 32LL);
      uint32_t cipherRight = cipher0[c] & 0xFFFFFFFFLL;
      cipherRight ^= (cipher1[c] & 0xFFFFFFFFLL);
      uint32_t Y = cipherRight;
      uint32_t Z = cipherLeft ^ outdiff;
      uint32_t fakeRight = cipher0[c] & 0xFFFFFFFFLL;
      uint32_t fakeLeft = cipher0[c] >> 32LL;
      uint32_t fakeRight2 = cipher1[c] & 0xFFFFFFFFLL;
      uint32_t fakeLeft2 = cipher1[c] >> 32LL;
      uint32_t Y0 = fakeRight;
      uint32_t Y1 = fakeRight2;
      int32_t fakeInput0 = Y0 ^ fakeK;
      uint32_t fakeInput1 = Y1 ^ fakeK;
      uint32_t fakeOut0 = fBox(fakeInput0);
      uint32_t fakeOut1 = fBox(fakeInput1);
      uint32_t fakeDiff = fakeOut0 ^ fakeOut1;
      if (fakeDiff == Z)
        score++;
      else
        break;
    }
    if (score == numPlain) {
      printf("found subkey : 0x%08lx\n", fakeK);
      return fakeK;
    }
  }
  printf("failed\n");
  return 0;
}

void chosenPlaintext(uint64_t diff, uint32_t seed) {
  printf("  Generating %i chosen-plaintext pairs\n", numPlain);
  printf("  Using input differential of 0x%016llx\n", diff);
  printf("SEED: %d\n", seed);
  srand(seed);
  for (int32_t c = 0; c < numPlain; c++) {
    plain0[c] = (rand() & 0xFFFFLL) << 48LL;
    plain0[c] += (rand() & 0xFFFFLL) << 32LL;
    plain0[c] += (rand() & 0xFFFFLL) << 16LL;
    plain0[c] += (rand() & 0xFFFFLL);
    cipher0[c] = encrypt(plain0[c]);
    plain1[c] = plain0[c] ^ diff;
    cipher1[c] = encrypt(plain1[c]);
    }
}

void undoLastRound(uint32_t crackedSubkey) {
  for (int32_t c = 0; c < numPlain; c++) {
    uint32_t cipherLeft0 = leftHalf(cipher0[c]);
    uint32_t cipherRight0 = rightHalf(cipher0[c]);
    uint32_t cipherLeft1 = leftHalf(cipher1[c]);
    uint32_t cipherRight1 = rightHalf(cipher1[c]);
    cipherLeft0 = cipherRight0;
    cipherLeft1 = cipherRight1;
    cipherRight0 = fBox(cipherLeft0 ^ crackedSubkey) ^ (cipher0[c] >> 32LL);
    cipherRight1 = fBox(cipherLeft1 ^ crackedSubkey) ^ (cipher1[c] >> 32LL);
    cipher0[c] = combineHalves(cipherLeft0, cipherRight0);
    cipher1[c] = combineHalves(cipherLeft1, cipherRight1);
  }
}

void prepForCrackingK0() {
  for (int32_t c = 0; c < numPlain; c++) {
    uint32_t cipherLeft0 = leftHalf(cipher0[c]);
    uint32_t cipherRight0 = rightHalf(cipher0[c]);
    uint32_t cipherLeft1 = leftHalf(cipher1[c]);
    uint32_t cipherRight1 = rightHalf(cipher1[c]);
    uint32_t tempLeft0 = cipherLeft0;
    uint32_t tempLeft1 = cipherLeft1;
    cipherLeft0 = cipherRight0;
    cipherLeft1 = cipherRight1;
    cipherRight0 = tempLeft0;
    cipherRight1 = tempLeft1;
    cipher0[c] = combineHalves(cipherLeft0, cipherRight0);
    cipher1[c] = combineHalves(cipherLeft1, cipherRight1);
  }
}

int32_t main() {
  printf("JK'S FEAL-4 DIFFERENTIAL CRYPTANALYSIS DEMO\n");
  printf("-------------------------------------------\n");
  printf("\n");
  int32_t graphData[20];
  int32_t c;
  time_t seed = 1459454193;
  printf("SEED: %d\n", seed);
  generateSubkeys(seed);
  numPlain = 12;
  uint64_t inputDiff1 = 0x8080000080800000LL;
  uint64_t inputDiff2 = 0x0000000080800000LL;
  uint64_t inputDiff3 = 0x0000000002000000LL;
  uint32_t outDiff = 0x02000000L;
  uint32_t fullStartTime = time(NULL);

  uint32_t seeds[] = {1459454193, 1459454392, 1459454479};

  // CRACKING ROUND 4
  printf("ROUND 4\n");
  chosenPlaintext(inputDiff1, seeds[0]);
  undoFinalOperation();
  uint32_t startTime = time(NULL);
  uint32_t crackedSubkey3 = crackLastRound(outDiff);
  uint32_t endTime = time(NULL);
  printf("  Time to crack round #4 = %i seconds\n", (endTime - startTime));

  // CRACKING ROUND 3
  printf("ROUND 3\n");
  chosenPlaintext(inputDiff2, seeds[1]);
  undoFinalOperation();
  undoLastRound(crackedSubkey3);
  startTime = time(NULL);
  uint32_t crackedSubkey2 = crackLastRound(outDiff);
  endTime = time(NULL);
  printf("  Time to crack round #3 = %i seconds\n", (endTime - startTime));

  // CRACKING ROUND 2
  printf("ROUND 2\n");
  chosenPlaintext(inputDiff3, seeds[2]);
  undoFinalOperation();
  undoLastRound(crackedSubkey3);
  undoLastRound(crackedSubkey2);
  startTime = time(NULL);
  uint32_t crackedSubkey1 = crackLastRound(outDiff);
  endTime = time(NULL);
  printf("  Time to crack round #2 = %i seconds\n", (endTime - startTime));

  // CRACK ROUND 1
  printf("ROUND 1\n");
  undoLastRound(crackedSubkey1);
  uint32_t crackedSubkey0 = 0;
  uint32_t crackedSubkey4 = 0;
  uint32_t crackedSubkey5 = 0;
  printf("  Cracking...");
  startTime = time(NULL);
  uint32_t guessK0;
  for (guessK0 = 0; guessK0 < 0xFFFFFFFFL; guessK0++) {/* loop2outer */
    uint32_t guessK4 = 0;
    uint32_t guessK5 = 0;
    int32_t c;
    for (c = 0; c < numPlain; c++) {/* loop2inner */
      uint32_t plainLeft0 = leftHalf(plain0[c]);
      uint32_t plainRight0 = rightHalf(plain0[c]);
      uint32_t cipherLeft0 = leftHalf(cipher0[c]);
      uint32_t cipherRight0 = rightHalf(cipher0[c]);
      uint32_t tempy0 = fBox(cipherRight0 ^ guessK0) ^ cipherLeft0;
      if (guessK4 == 0) {
        guessK4 = tempy0 ^ plainLeft0;
        guessK5 = tempy0 ^ cipherRight0 ^ plainRight0;
      } else if (((tempy0 ^ plainLeft0) != guessK4)
               || ((tempy0 ^ cipherRight0 ^ plainRight0) != guessK5)) {
        guessK4 = 0;
        guessK5 = 0;
        break;
      }
    }
    if (guessK4 != 0) {
      crackedSubkey0 = guessK0;
      crackedSubkey4 = guessK4;
      crackedSubkey5 = guessK5;
      endTime = time(NULL);
      printf("found subkeys : 0x%08lx  0x%08lx  0x%08lx\n", guessK0, guessK4,
                guessK5);
      printf("  Time to crack round #1 = %i seconds\n", (endTime - startTime));
      break;
    }
  }
  printf("\n\n");
  printf("0x%08lx - ", crackedSubkey0);
  if (crackedSubkey0 == subkey[0])
    printf("Subkey 0 : GOOD!\n");
  else
    printf("Subkey 0 : BAD\n");
  printf("0x%08lx - ", crackedSubkey1);
  if (crackedSubkey1 == subkey[1])
    printf("Subkey 1 : GOOD!\n");
  else
    printf("Subkey 1 : BAD\n");
  printf("0x%08lx - ", crackedSubkey2);
  if (crackedSubkey2 == subkey[2])
    printf("Subkey 2 : GOOD!\n");
  else
    printf("Subkey 2 : BAD\n");
  printf("0x%08lx - ", crackedSubkey3);
  if (crackedSubkey3 == subkey[3])
    printf("Subkey 3 : GOOD!\n");
  else
    printf("Subkey 3 : BAD\n");
  printf("0x%08lx - ", crackedSubkey4);
  if (crackedSubkey4 == subkey[4])
    printf("Subkey 4 : GOOD!\n");
  else
    printf("Subkey 4 : BAD\n");
  printf("0x%08lx - ", crackedSubkey5);
  if (crackedSubkey5 == subkey[5])
    printf("Subkey 5 : GOOD!\n");
  else
    printf("Subkey 5 : BAD\n");
  printf("\n");
  uint32_t fullEndTime = time(NULL);
  printf("Total crack time = %i seconds\n", (fullEndTime - fullStartTime));
  printf("FINISHED\n");
  return 0;
}
