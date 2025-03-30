from eth_utils import (
    to_checksum_address,
    keccak,
    to_hex
)
from eth_account import Account

private_key_10 = "0xdfcaa366cd21e9c6b70cff6ef14d1206401065647ffcac3f2fda9bb309678e54"

valid_private_key_11 = "0x8c2e65105a2ed72d44b102da392787c3742314b33e0cba814de6a9128a00e88c"
account = Account.from_key(private_key_10)

print("Derived Address:", account.address)  # Should match the given address