file(GLOB TEST_FILES "*.cpp")

add_executable(tests ${TEST_FILES})
target_link_libraries(tests PRIVATE customer_core gtest gtest_main pthread)

add_test(NAME run_all_tests COMMAND tests)
