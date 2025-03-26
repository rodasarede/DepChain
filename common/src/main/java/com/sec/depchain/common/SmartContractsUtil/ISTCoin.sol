// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "node_modules/@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "common/src/main/java/com/sec/depchain/common/SmartContractsUtil/Blacklist.sol";

contract ISTCoin is ERC20, Blacklist {

    constructor() ERC20("IST Coin", "IST") Blacklist() {
        _mint(msg.sender, 100_000_000 * 10 ** decimals());
    }

    function decimals() public view virtual override returns (uint8) {
        return 2;
    }

    function transfer(address recipient, uint256 amount) public override returns (bool) {
        require(!super.isBlacklisted(msg.sender), "Sender is blacklisted");
        require(!super.isBlacklisted(recipient), "Recipient is blacklisted");
        return super.transfer(recipient, amount);
    }

    function transferFrom(address sender, address recipient, uint256 amount) public override returns (bool) {
        require(!super.isBlacklisted(sender), "Sender is blacklisted");
        require(!super.isBlacklisted(recipient), "Recipient is blacklisted");
        return super.transferFrom(sender, recipient, amount);
    }
}