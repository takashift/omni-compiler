program tgmove
integer i0,i1,i2,n=4
integer a(n,n,n),b(n,n,n)
!$xmp nodes p(2)
!$xmp template tx(n)
!$xmp distribute tx(block) onto p
!$xmp align a(i,*,*) with tx(i)
!$xmp align b(*,i,*) with tx(i)

!$xmp loop (i0) on tx(i0)
do i2=1,n
  do i1=1,n
    do i0=1,n
      a(i0,i1,i2)=i0+i1+i2
    end do
  end do
end do

do i2=1,n
!$xmp loop (i1) on tx(i1)
  do i1=1,n
    do i0=1,n
      b(i0,i1,i2)=0
    end do
  end do
end do

!$xmp gmove
b(1:n,1:n,1:n)=a(1:n,1:n,1:n)

ierr=0
do i2=1,n
!$xmp loop (i1) on tx(i1)
  do i1=1,n
!$xmp loop (i0) on tx(i0)
    do i0=1,n
      ierr=ierr+abs(b(i0,i1,i2)-a(i0,i1,i2))
    end do
  end do
end do

print *, 'max error=',ierr

contains
end
