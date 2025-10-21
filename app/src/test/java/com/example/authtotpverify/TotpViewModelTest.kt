package com.example.authtotpverify

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TotpViewModelTest {

    private val testDispatcher = StandardTestDispatcher()


    @Before
    fun setup(){
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown(){
        Dispatchers.resetMain()
    }

    @Test
    fun addEntrySuccess() {
        val vm = TotpViewModel(30,6)
        val secret = "ABC"
        val issuer = "issuer1"

        val result = vm.addNewEntry(secret, issuer)

        assertThat(result).isTrue()
        assertThat(vm.userEntries).hasSize(1)
        assertThat(vm.userEntries[0].secret).isEqualTo(secret)
        assertThat(vm.userEntries[0].issuer).isEqualTo(issuer)
        assertThat(vm.userEntries[0].code.length).isEqualTo(vm.digits)
    }

    @Test
    fun addEntrySuccessEmptyIssuer() {
        val vm = TotpViewModel(30,6)
        val secret = "ABC"
        val issuer = ""

        val result = vm.addNewEntry(secret, issuer)

        assertThat(result).isTrue()
        assertThat(vm.userEntries).hasSize(1)
        assertThat(vm.userEntries[0].secret).isEqualTo(secret)
        assertThat(vm.userEntries[0].issuer).isEqualTo("Unknown")
        assertThat(vm.userEntries[0].code.length).isEqualTo(vm.digits)
    }

    @Test
    fun addEntryFail() {
        val vm = TotpViewModel(30,6)
        val secret = ""
        val issuer = "issuer1"

        val result = vm.addNewEntry(secret, issuer)

        assertThat(result).isFalse()
        assertThat(vm.userEntries).hasSize(0)
    }

    @Test
    fun addEntryFailDuplicateEntry() {
        val vm = TotpViewModel(30,6)
        val secret = "ABC"
        vm.addNewEntry(secret, "issuer1")

        val second = vm.addNewEntry(" abc ", "issuer2")
        assertThat(second).isFalse()
        assertThat(vm.userEntries).hasSize(1)
    }


}