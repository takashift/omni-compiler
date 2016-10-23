#include <xmp.h>
#include <stdio.h>
#include <stdbool.h>
#include <stdlib.h>
#define N 10
#define NB 4
#define NPROW 2
#define NPCOL 2
int A[N][N], sol[N];
#pragma xmp nodes p(NPROW,NPCOL)
#pragma xmp template t(0:N-1,0:N-1)
#pragma xmp distribute t(cyclic(NB),cyclic(NB)) onto p
#pragma xmp align A[i][j] with t(j,i)

#pragma xmp template t_b(0:N-1,0:NPCOL-1)
#pragma xmp distribute t_b(cyclic(NB),block) onto p
#pragma xmp align sol[j] with t_b(j,*)

int main()
{
#pragma xmp loop (i,j) on t(i,j)
  for(int j=0;j<N;j++)
    for(int i=0;i<N;i++)
      A[j][i] = j * N + i;

#pragma xmp loop on t_b(i,*)
  for(int i=0;i<N;i++)
    sol[i] = -9;

#pragma xmp gmove
  sol[2:5] = A[3][2:5];

  int flag = false;
  if(xmp_node_num() == 1 || xmp_node_num() == 3){
    if(sol[2] == 32 && sol[3] == 33)
      flag = true;
  }
  else{
    if(sol[4] == 34 && sol[5] == 35 && sol[6] == 36)
      flag = true;
  }
#pragma xmp reduction(min:flag)
#pragma xmp task on p(1,1)
  if(flag == false){
    printf("ERROR in gmove_opt_block_cyclic\n");
    exit(1);
  }
  else
    printf("PASS gmove_opt_block_cyclic\n");
  
  return 0;
}

