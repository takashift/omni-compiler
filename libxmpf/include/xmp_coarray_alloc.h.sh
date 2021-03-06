#!/bin/bash

#-------------------------------------------------------
#  generator for xmp_lib_coarray_alloc.h
#-------------------------------------------------------

#--------------------
#  sub
#--------------------
echo72 () {
    str="$1                                                                        "
    str=`echo "$str" | cut -c -72`"&"
    echo "$str"
}


print_subr_malloc() {
    tk=$1
    typekind=$2

    echo72     "      subroutine xmpf_coarray_malloc${DIM}d_${tk}(descptr, var, count,"

 case "${DIM}" in
 0) echo       "     &   element, tag, rank)" ;;
 *) echo72     "     &   element, tag, rank"
    for i in `seq 1 ${DIM}`; do
        echo72 "     &   , lb${i}, ub${i}"
    done
    echo       "     &   )" ;;
 esac

    echo       "        integer(8), intent(in) :: descptr"
    echo       "        integer(8), intent(in) :: tag"
    echo       "        integer, intent(in) :: count, element, rank"

 case "${DIM}" in
 0) ;;
 *) for i in `seq 1 ${DIM}`; do
        echo   "        integer, intent(in) :: lb${i}, ub${i}"
    done ;;
 esac

 case "${DIM}" in
 0) echo    "        ${typekind}, pointer, intent(inout) :: var" ;;
 1) echo    "        ${typekind}, pointer, intent(inout) :: var(:)" ;;
 *) echo -n "        ${typekind}, pointer, intent(inout) :: var(:" 
    for i in `seq 2 ${DIM}`; do
        echo -n ",:"
    done
    echo    ")" ;;
 esac

    echo    "      end subroutine"
    echo
}


print_subr_regmem() {
    tk=$1
    typekind=$2

    echo72     "      subroutine xmpf_coarray_regmem${DIM}d_${tk}(descptr, var, count,"

 case "${DIM}" in
 0) echo       "     &   element, tag, rank)" ;;
 *) echo72     "     &   element, tag, rank"
    for i in `seq 1 ${DIM}`; do
        echo72 "     &   , lb${i}, ub${i}"
    done
    echo       "     &   )" ;;
 esac

    echo       "        integer(8), intent(in) :: descptr"
    echo       "        integer(8), intent(in) :: tag"
    echo       "        integer, intent(in) :: count, element, rank"

 case "${DIM}" in
 0) ;;
 *) for i in `seq 1 ${DIM}`; do
        echo   "        integer, intent(in) :: lb${i}, ub${i}"
    done ;;
 esac

 case "${DIM}" in
 0) echo    "        ${typekind}, intent(inout) :: var" ;;
 1) echo    "        ${typekind}, intent(inout) :: var(:)" ;;
 *) echo -n "        ${typekind}, intent(inout) :: var(:" 
    for i in `seq 2 ${DIM}`; do
        echo -n ",:"
    done
    echo    ")" ;;
 esac

    echo    "      end subroutine"
    echo
}


print_subr_dealloc() {
    tk=$1
    typekind=$2

    echo    "      subroutine xmpf_coarray_dealloc${DIM}d_${tk}(descptr, var)"
    echo    "        integer(8), intent(in) :: descptr"

 case "${DIM}" in
 0) echo    "        ${typekind}, pointer, intent(inout) :: var" ;;
 1) echo    "        ${typekind}, pointer, intent(inout) :: var(:)" ;;
 *) echo -n "        ${typekind}, pointer, intent(inout) :: var(:"
    for i in `seq 2 ${DIM}`; do
        echo -n ",:"
    done
    echo    ")" ;;
 esac

    echo    "      end subroutine"
    echo
}


# NOT USED
# The translated program calls xmpf_coarray_deregmem_() directly.
print_subr_deregmem() {
    tk=$1
    typekind=$2

    echo    "      subroutine xmpf_coarray_deregmem${DIM}d_${tk}(descptr, var)"
    echo    "        integer(8), intent(in) :: descptr"

 case "${DIM}" in
 0) echo    "        ${typekind}, intent(inout) :: var" ;;
 1) echo    "        ${typekind}, intent(inout) :: var(:)" ;;
 *) echo -n "        ${typekind}, intent(inout) :: var(:"
    for i in `seq 2 ${DIM}`; do
        echo -n ",:"
    done
    echo    ")" ;;
 esac

    echo    "      end subroutine"
    echo
}


#--------------------
#  main
#--------------------
echo "!! This file is automatically generated by $0"
echo '!!'
echo '!! RESTRICTIONS in XMP/F'
echo '!!  - Quadruple precision real and complex are not supported.'
echo '!!  - The number of array dimensions cannot exceed 7.'
echo '!!'
echo ''
echo '!-----------------------------------------------------------------------'
echo '      interface xmpf_coarray_malloc_generic'
echo '!-----------------------------------------------------------------------'
echo ''

TARGET=$1
for DIM in `seq 0 7`
do
    if test "sxace-nec-superux" != "$TARGET"; then    ## integer(2) cannot be used on SX-ACE
	print_subr_malloc i2  "integer(2)"
    fi
    print_subr_malloc i4  "integer(4)"      
    print_subr_malloc i8  "integer(8)"
    if test "sxace-nec-superux" != "$TARGET"; then    ## logical(2) cannot be used on SX-ACE
	print_subr_malloc l2  "logical(2)"
    fi
    print_subr_malloc l4  "logical(4)"      
    print_subr_malloc l8  "logical(8)"      
    print_subr_malloc r4  "real(4)"         
    print_subr_malloc r8  "real(8)"         
    print_subr_malloc z8  "complex(4)"      
    print_subr_malloc z16 "complex(8)"      
    print_subr_malloc cn  "character(element)" 
done

echo '      end interface'
echo ''
echo '!-----------------------------------------------------------------------'
echo '      interface xmpf_coarray_regmem_generic'
echo '!-----------------------------------------------------------------------'
echo ''

TARGET=$1
for DIM in `seq 0 7`
do
    if test "sxace-nec-superux" != "$TARGET"; then    ## integer(2) cannot be used on SX-ACE
	print_subr_regmem i2  "integer(2)"
    fi
    print_subr_regmem i4  "integer(4)"      
    print_subr_regmem i8  "integer(8)"
    if test "sxace-nec-superux" != "$TARGET"; then    ## logical(2) cannot be used on SX-ACE
	print_subr_regmem l2  "logical(2)"
    fi
    print_subr_regmem l4  "logical(4)"      
    print_subr_regmem l8  "logical(8)"      
    print_subr_regmem r4  "real(4)"         
    print_subr_regmem r8  "real(8)"         
    print_subr_regmem z8  "complex(4)"      
    print_subr_regmem z16 "complex(8)"      
    print_subr_regmem cn  "character(element)" 
done

echo '      end interface'
echo ''
echo '!-----------------------------------------------------------------------'
echo '      interface xmpf_coarray_dealloc_generic'
echo '!-----------------------------------------------------------------------'
echo ''

for DIM in `seq 0 7`
do
    if test "sxace-nec-superux" != "$TARGET"; then    ## integer(2) cannot be used on SX-ACE
	print_subr_dealloc i2  "integer(2)"
    fi
    print_subr_dealloc i4  "integer(4)"      
    print_subr_dealloc i8  "integer(8)"
    if test "sxace-nec-superux" != "$TARGET"; then    ## logical(2) cannot be used on SX-ACE
	print_subr_dealloc l2  "logical(2)"
    fi
    print_subr_dealloc l4  "logical(4)"      
    print_subr_dealloc l8  "logical(8)"      
    print_subr_dealloc r4  "real(4)"         
    print_subr_dealloc r8  "real(8)"         
    print_subr_dealloc z8  "complex(4)"      
    print_subr_dealloc z16 "complex(8)"      
    print_subr_dealloc cn  "character(*)" 
done

echo '      end interface'
echo ''

exit
